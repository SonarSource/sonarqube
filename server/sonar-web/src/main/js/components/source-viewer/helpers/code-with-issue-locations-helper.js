/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import escapeHtml from 'escape-html';

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
 * @param {string} rootClassName
 * @returns {Array}
 */
function splitByTokens (code, rootClassName = '') {
  const container = document.createElement('div');
  let tokens = [];
  container.innerHTML = code;
  [].forEach.call(container.childNodes, node => {
    if (node.nodeType === 1) {
      // ELEMENT NODE
      const fullClassName = rootClassName ? (rootClassName + ' ' + node.className) : node.className;
      const innerTokens = splitByTokens(node.innerHTML, fullClassName);
      tokens = tokens.concat(innerTokens);
    }
    if (node.nodeType === 3) {
      // TEXT NODE
      tokens.push({ className: rootClassName, text: node.nodeValue });
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
  issueLocations.forEach(location => {
    const nextTokens = [];
    let acc = 0;
    tokens.forEach(token => {
      const x = intersect(acc, acc + token.text.length, location.from, location.to);
      const p1 = part(token.text, acc, x.from, acc);
      const p2 = part(token.text, x.from, x.to, acc);
      const p3 = part(token.text, x.to, acc + token.text.length, acc);
      if (p1.length) {
        nextTokens.push({ className: token.className, text: p1 });
      }
      if (p2.length) {
        const newClassName = token.className.indexOf(className) === -1 ?
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
  return tokens.map(token => (
      `<span class="${token.className}">${escapeHtml(token.text)}</span>`
  )).join('');
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
  const _code = code || '&nbsp;';
  const _issueLocations = issueLocations || [];
  const _className = optionalClassName ? optionalClassName : 'source-line-code-issue';
  return generateHTML(highlightIssueLocations(splitByTokens(_code), _issueLocations, _className));
}

export default doTheStuff;

