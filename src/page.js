module.exports = function processPage(page) {
    page.content = maybeMoveCodeSnippet(page.content);
    return page;
}

function maybeMoveCodeSnippet(content) {
    return content.replace(/```[^\r\n]*[\r\n]*({% lanying_code_snippet[^\r\n]*endlanying_code_snippet %})([ \r\n]*)```/g, (match, group1, group2) => `\n${group1}\n`).replace(/({% lanying_code_snippet[^\r\n]*endlanying_code_snippet %})([ \r\n]*)```/g, (match, group1, group2) => `${group2}\`\`\`\n\n${group1}\n`);
}
