const {spawn} = require('child_process');
var resolve = require('path').resolve
var repoCache = {}
var deasync = require('deasync');
module.exports = function init() {
    const options = this.config.get('pluginsConfig')['lanying-code-snippet']
    var logger = this.log
    repoList = options['repositories'] || []
    repoList.forEach(
        repo => {
            if (!repoCache[repo.name]){
                var isFinish = false
                var child = spawn("joern", ["--script", `${process.cwd()}/node_modules/gitbook-plugin-lanying-code-snippet/src/init.sc`,
                "--param", `name=${repo.name}`,
                "--param", `url=${repo.url}`,
                "--param", `branch=${repo.branch}`,
                "--param", `cacheDir=${repo.cacheDir ? resolve(repo.cacheDir) : ""}`], {cwd: "/tmp"})
                child.stdout.on('data', data => {
                    logger.debug.ln(data.toString().replace(/\n+$/, ""))
                })
                child.stderr.on('data', data => {
                    logger.debug.ln(data.toString().replace(/\n+$/, ""))
                })
                child.on('exit', function () {
                    isFinish = true
                })
                repoCache[repo.name] = repo
                while(!isFinish){
                    deasync.runLoopOnce();
                }
                logger.info.ln("init finish:", repo.name)
            }
        }
    )
}
