const {spawn} = require('child_process');
var resolve = require('path').resolve
var repoCache = {}
var deasync = require('deasync');
module.exports = function init() {
    const options = this.config.get('pluginsConfig')['lanying-code-snippet']
    repoList = options['repositories'] || []
    repoList.forEach(
        repo => {
            if (!repoCache[repo.name]){
                var isFinish = false
                var child = spawn("joern", ["--script", `${process.cwd()}/node_modules/gitbook-plugin-lanying-code-snippet/src/init.sc`, "--params", `name=${repo.name},url=${repo.url},branch=${repo.branch},cacheDir=${repo.cacheDir ? resolve(repo.cacheDir) : ""}`], {cwd: "/tmp"})
                child.stdout.on('data', data => {
                    console.log(data.toString().replace(/\n+$/, ""))
                })
                child.stderr.on('data', data => {
                    console.log(data.toString().replace(/\n+$/, ""))
                })
                child.on('exit', function () {
                    isFinish = true
                })
                repoCache[repo.name] = repo
                while(!isFinish){
                    deasync.runLoopOnce();
                }
                console.log("init finish:", repo.name)
            }
        }
    )
}
