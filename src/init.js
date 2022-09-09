var exec = require("child_process").execSync
var repoCache = {}
module.exports = function init() {
    const options = this.config.get('pluginsConfig')['lanying-code-snippet']
    repoList = options['repositories'] || []
    repoList.forEach(
        repo => {
            if (!repoCache[repo.name]){
                var cmd = `joern --script ${process.cwd()}/node_modules/gitbook-plugin-lanying-code-snippet/src/init.sc --params name=${repo.name},url=${repo.url},branch=${repo.branch}` 
                var result = exec(cmd, {cwd: "/tmp"}).toString().trim()
                repoCache[repo.name] = repo
            }
        }
    )
}
