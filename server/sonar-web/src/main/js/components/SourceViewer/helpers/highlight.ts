/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { uniq } from 'lodash';

export interface Token {
  className: string;
  markers: number[];
  text: string;
}

const ISSUE_LOCATION_CLASS = 'source-line-code-issue';

export function splitByTokens(code: string, rootClassName = ''): Token[] {
  const container = document.createElement('div');
  let tokens: Token[] = [];
  container.innerHTML = code;
  [].forEach.call(container.childNodes, (node: Element) => {
    if (node.nodeType === 1) {
      // ELEMENT NODE
      const fullClassName = rootClassName ? rootClassName + ' ' + node.className : node.className;
      const innerTokens = splitByTokens(node.innerHTML, fullClassName);
      tokens = tokens.concat(innerTokens);
    }
    if (node.nodeType === 3 && node.nodeValue) {
      // TEXT NODE
      tokens.push({ className: rootClassName, markers: [], text: node.nodeValue });
    }
  });
  return tokens;
}

export function highlightSymbol(tokens: Token[], symbol: string): Token[] {
  const symbolRegExp = new RegExp(`\\b${symbol}\\b`);
  return tokens.map(token =>
    symbolRegExp.test(token.className)
      ? { ...token, className: `${token.className} highlighted` }
      : token
  );
}

/**
 * Intersect two ranges
 * @param s1 Start position of the first range
 * @param e1 End position of the first range
 * @param s2 Start position of the second range
 * @param e2 End position of the second range
 */
function intersect(s1: number, e1: number, s2: number, e2: number) {
  return { from: Math.max(s1, s2), to: Math.min(e1, e2) };
}

/**
 * Get the substring of a string
 * @param str A string
 * @param from "From" offset
 * @param to "To" offset
 * @param acc Global offset to eliminate
 */
function part(str: string, from: number, to: number, acc: number): string {
  // we do not want negative number as the first argument of `substr`
  return from >= acc ? str.substr(from - acc, to - from) : str.substr(0, to - from);
}

/**
 * Highlight issue locations in the list of tokens
 */
export function highlightIssueLocations(
  tokens: Token[],
  issueLocations: T.LinearIssueLocation[],
  rootClassName: string = ISSUE_LOCATION_CLASS
): Token[] {
  issueLocations.forEach(location => {
    const nextTokens: Token[] = [];
    let acc = 0;
    let markerAdded = location.line !== location.startLine;
    tokens.forEach(token => {
      const x = intersect(acc, acc + token.text.length, location.from, location.to);
      const p1 = part(token.text, acc, x.from, acc);
      const p2 = part(token.text, x.from, x.to, acc);
      const p3 = part(token.text, x.to, acc + token.text.length, acc);
      if (p1.length) {
        nextTokens.push({ ...token, text: p1 });
      }
      if (p2.length) {
        const newClassName =
          token.className.indexOf(rootClassName) === -1
            ? `${token.className} ${rootClassName}`
            : token.className;
        nextTokens.push({
          className: newClassName,
          markers:
            !markerAdded && location.index != null
              ? uniq([...token.markers, location.index])
              : token.markers,
          text: p2
        });
        markerAdded = true;
      }
      if (p3.length) {
        nextTokens.push({ ...token, text: p3 });
      }
      acc += token.text.length;
    });
    tokens = nextTokens.slice();
  });
  return tokens;
}
