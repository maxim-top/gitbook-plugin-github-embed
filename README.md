# Embed Github Snippets into Gitbooks

Just fill in the repository name, class name and function name, this plugin will automatically search the github repository to generate code snippets that call this function。


Using this tag `{% lanying_code_snippet repo="lanying-im-web",class="userManage",function="asyncRegister" %}{% endlanying_code_snippet %}`, it will generate the code snippet as shown below:
![](demo.png)

## Prerequisites
[Joern](https://github.com/joernio/joern) needs to be installed.

You can install [Joern](https://github.com/joernio/joern) according to [this document](https://docs.joern.io/installation)

## Installation
Add the following plugins to your book.json and run gitbook install

{
    "plugins": ["lanying-code-snippet"]
}

## Usage
Configuration option can be set as an obj like:
```
{
    "plugins": [
        "lanying-code-snippet"
    ],
    "pluginsConfig": {
        "lanying-code-snippet": {
            "showLink": true,
            "reindent": true,
            "maxLine": 20,
            "maxSnippetCount": 10,
            "repositories": [
                {
                    "name":"lanying-im-web",
                    "url":"https://github.com/maxim-top/lanying-im-web.git",
                    "branch":"master"
                },
                {
                    "name":"lanying-im-android",
                    "url":"https://github.com/maxim-top/lanying-im-android.git",
                    "branch":"master"
                }
            ]
        }
    }
}
```
## Configuration

* `showLink=true` Show a link below the embedded source back to the source file. Defaults to `true`
* `reindent=true` Re-indent the lines given the line numbers. Defaults to `true`
* `maxLine=20` maximum number of lines each code snippet. Defaults to `20`
* `maxSnippetCount=10` Maximum number of code snippets. Defaults to `10`
* `repositories` repositories are used to configure github repository information: `repositories[*].url` is the url of github repository, `repositories[*].branch` is the branch of github repository, `repositories[*].name` is the name of github repository.
* Using this tag `{% lanying_code_snippet repo="lanying-im-web",class="userManage",function="asyncRegister" %}{% endlanying_code_snippet %}` to generate the code snippets, the `repo` is the name of github repository, must be one of  `repositories[*].name`, the `class` is the class name, the `function` is the function name.

## Styling the Link

Use a gitbook style override to adjust the style of the link. The class is [`.lanying-code-snippet-caption`](https://github.com/maxim-top/gitbook-plugin-lanying-code-snippet/blob/master/book/lanying-code-snippet.css).

## FAQ
* When gitbook install report `Error: spawn E2BIG`, should rerun gitbook install command.
