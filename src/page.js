module.exports = function processPage(page) {
    page.content = maybeMoveCodeSnippet(page.content);
    return page;
}

function maybeMoveCodeSnippet(content) {
    return content.replaceAll(/```[^\r\n]*[\r\n]*({% lanying_code_snippet[^\r\n]*endlanying_code_snippet %})([ \r\n]*)```/g, '\n$1\n').replaceAll(/({% lanying_code_snippet[^\r\n]*endlanying_code_snippet %})([ \r\n]*)```/g, '$2```\n\n$1\n')
}
