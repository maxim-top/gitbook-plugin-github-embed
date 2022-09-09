var { EOL } = require('os');
var WHITESPACE = /^[^\S\n]*(?=[\S]+)/mg;

var api = {
    shortest: function(src) {
        // Any content line has no prefix whitespace, do nothing
        if (src.match(/^[\S]/mg)) return src;

        var match = src.match(WHITESPACE)

        var shortest = Number.MAX_VALUE
        if (match) {
            match.forEach(prefix => {
                var len = prefix.length
                shortest = Math.max(0, len < shortest ? len : shortest);
            })
        }
        return shortest;
    },

    trimmer: function(src) {
        var shortest = api.shortest(src);

        if (shortest !== Number.MAX_VALUE && shortest > 0) {
            var shorten = new RegExp('^\\s{' + shortest + '}', '')
            var reindented = src.replace(WHITESPACE, prefix => {
                return prefix.replace(shorten, '')
            })

            return reindented;
        }

        return src
    }
}


module.exports = api
