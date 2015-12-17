import _ from 'underscore';

/**
 * Intersect two ranges
 * @param {number} s1 Start position of the first range
 * @param {number} e1 End position of the first range
 * @param {number} s2 Start position of the second range
 * @param {number} e2 End position of the second range
 * @returns {{from: number, to: number}}
 */
function intersect (s1, e1, s2, e2) {
  return { from: Math.max(s1, s2), to: Math.min(e1, e2) };
}


/**
 * Get the substring of a string
 * @param {string} str A string
 * @param {number} from "From" offset
 * @param {number} to "To" offset
 * @param {number} acc Global offset to eliminate
 * @returns {string}
 */
function part (str, from, to, acc) {
  // we do not want negative number as the first argument of `substr`
  return from >= acc ? str.substr(from - acc, to - from) : str.substr(0, to - from);
}


/**
 * Split a code html into tokens
 * @param {string} code
 * @returns {Array}
 */
function splitByTokens (code) {
  var container = document.createElement('div'),
      tokens = [];
  container.innerHTML = code;
  [].forEach.call(container.childNodes, function (node) {
    if (node.nodeType === 1) {
      // ELEMENT NODE
      tokens.push({ className: node.className, text: node.textContent });
    }
    if (node.nodeType === 3) {
      // TEXT NODE
      tokens.push({ className: '', text: node.nodeValue });
    }
  });
  return tokens;
}


/**
 * Highlight issue locations in the list of tokens
 * @param {Array} tokens
 * @param {Array} issueLocations
 * @param {string} className
 * @returns {Array}
 */
function highlightIssueLocations (tokens, issueLocations, className) {
  issueLocations.forEach(function (location) {
    var nextTokens = [],
        acc = 0;
    tokens.forEach(function (token) {
      var x = intersect(acc, acc + token.text.length, location.from, location.to);
      var p1 = part(token.text, acc, x.from, acc),
          p2 = part(token.text, x.from, x.to, acc),
          p3 = part(token.text, x.to, acc + token.text.length, acc);
      if (p1.length) {
        nextTokens.push({ className: token.className, text: p1 });
      }
      if (p2.length) {
        var newClassName = token.className.indexOf(className) === -1 ?
            [token.className, className].join(' ') : token.className;
        nextTokens.push({ className: newClassName, text: p2 });
      }
      if (p3.length) {
        nextTokens.push({ className: token.className, text: p3 });
      }
      acc += token.text.length;
    });
    tokens = nextTokens.slice();
  });
  return tokens;
}


/**
 * Generate an html string from the list of tokens
 * @param {Array} tokens
 * @returns {string}
 */
function generateHTML (tokens) {
  return tokens.map(function (token) {
    return '<span class="' + token.className + '">' + _.escape(token.text) + '</span>';
  }).join('');
}


/**
 * Take the initial source code, split by tokens,
 * highlight issues and generate result html
 * @param {string} code
 * @param {Array} issueLocations
 * @param {string} [optionalClassName]
 * @returns {string}
 */
function doTheStuff (code, issueLocations, optionalClassName) {
  var _code = code || '&nbsp;';
  var _issueLocations = issueLocations || [];
  var _className = optionalClassName ? optionalClassName : 'source-line-code-issue';
  return generateHTML(highlightIssueLocations(splitByTokens(_code), _issueLocations, _className));
}


export default doTheStuff;


