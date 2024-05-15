var { trimmer } = require('./trimmer');
var { Encoder } = require('node-html-encoder');
var entityEncoder = new Encoder('entity');
const {spawn} = require('child_process');
var exec = require("child_process").execSync
var repoHeadCache = {}
var repoCache = {}
var deasync = require('deasync');
var resolve = require('path').resolve

module.exports = function processGithubEmbed(block) {
    var logger = this.log
    const pluginOptions = this.config.get('pluginsConfig')['lanying-code-snippet']
    var options = block.kwargs || {}
    options.logger = logger
    if(!repoIsExist(options, pluginOptions)){
        logger.warn.ln(`repo ${options.repo} not found, so skip.`)
        return "";
    }
    return extractCodeSnippet({...pluginOptions, ...options})
}

function repoIsExist(options, pluginOptions) {
    var repoList = pluginOptions['repositories'] || []
    var hasRepo = false
    repoList.forEach(repo => {
        if (repo.name == options.repo){
            hasRepo = true
        }
    })
    return hasRepo
}
function extractCodeSnippet(options) {
    var logger = options.logger
    var child = repoCache[options.repo]
    if (!child){
        child = spawn("joern", ["--script",`${process.cwd()}/node_modules/gitbook-plugin-lanying-code-snippet/src/tag.sc`,"--params",`repo=${options.repo}`],{cwd: "/tmp"})
        repoCache[options.repo] = child
    }
    var lineDelimiter = "__LANYING_CODE_SNAPPET_LINE_DELIMITER__"
    var fieldDelimiter = "__LANYING_CODE_SNAPPET_FIELD_DELIMITER__"
    var html = ''
    var lineCache = {}
    var isFinish = false
    var count = 0

    child.on('exit', function(code){
        isFinish = true
    })

    var repoList = options['repositories'] || []
    var repo =  {}
    repoList.forEach(nowRepo => {
        if (nowRepo.name == options.repo){
            repo = nowRepo
        }
    })
    var filter = options.filter || repo.filter || "call|override"

    logger.debug.ln(`processing code snippet: repo=${repo.name} class=${options.class}, function=${options.function}`)
    child.stdin.setEncoding('utf-8');
    child.stdin.write(`ExtractCode ${options.class} ${options.function} ${options.maxLine || 20} ${options.maxSnippetCount || 5} ${repo.cacheDir ? resolve(repo.cacheDir) : "no-cache-dir"} ${filter}\r\n`)
    child.stdout.on('data', data => {
        lines = data.toString().trim().split('\n')
        lines.forEach(line => {
            var fields = line.split(fieldDelimiter)
            //console.log("Line:", fields)
            if (fields.length == 5 && fields[0] == "CodeSnippet"){
                var fileName = fields[1]
                var line = fields[2]
                var code = fields[3].replace(new RegExp(lineDelimiter, 'g'), '\n').replace(/^({)/, '').replace(/^(\n)/, '').replace(/(})$/, '');
                var repoPath = fields[4]
                var head = repoHeadCache[options.repo]
                if (!head) {
                    var getHeadCmd = `cd ${repoPath} && git rev-parse HEAD`
                    var getHeadResult = exec(getHeadCmd).toString().trim()
                    head = getHeadResult.split('\n')[0]
                    repoHeadCache[options.repo] = head
                }
                if (!lineCache[`${fileName}|${line}`]) {
                    count++
                    html += transformCodeSnippet(options, fileName, line, code, head)
                    lineCache[`${fileName}|${line}`] = true
                }
            }else if(fields.length >= 1 && fields[0] == "ExtractCodeFinish"){
                isFinish = true
            }
        })
    })
    while(!isFinish){
        deasync.runLoopOnce();
    }
    child.stdout.removeAllListeners('data')
    child.removeAllListeners('exit')
    logger.debug.ln(`found ${count} snippets`)
    return html
}

function transformCodeSnippet(options, fileName, line,  code, head) {
    var trimmed = code.replace(/[\s\n\r]*$/g, '')
    var language = '';
    var link = ''
    var repoList = options['repositories'] || []
    var repoUrl = ''
    repoList.forEach(repo => {
        if (repo.name == options.repo){
            repoUrl = repo.url
        }
    })

    var url = ''
    if (repoUrl.startsWith("git@")) {
        url = repoUrl.replace(/:/g, '/').replace('git@', 'https://').replace(/\.git/g, '');
    } else {
        url = repoUrl.replace(/\.git/g, '');
    }
    url = `${url}/blob/${head}/${fileName}#L${line}`

    if (options.reindent !== false) {
        trimmed = trimmer(trimmed)
    }

    var extension = fileName.split('.').pop();
    if (extension) {
        if (extension == 'vue'){
            language = 'lang-js'
        } else if (extension == 'cc'){
            language = 'lang-c'
        } else if (extension == 'h'){
            language = 'lang-c'
        } else if (extension == 'm'){
            language = 'lang-c'
        } else if (extension == 'mm'){
            language = 'lang-c'
        } else {
            language = 'lang-' + extension
        }
    }

    if (options.showLink !== false) {
        link = `<div class="lanying-code-snippet-caption"><a title="Show Full Source of ${fileName}" href="${url}">Github Source: ${fileName} (line ${line})</a></div>`;
    }
    return `<pre><code class="${language}">${entityEncoder.htmlEncode(trimmed)}</code></pre>${link}`
}
