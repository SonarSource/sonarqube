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
import { sortBy } from 'lodash';
import { MarkdownRemark } from '../@types/graphql-types';

const WORDS = 6;

function cutLeadingWords(str: string) {
  let words = 0;
  for (let i = str.length - 1; i >= 0; i--) {
    if (/\s/.test(str[i])) {
      words++;
    }
    if (words === WORDS) {
      return i > 0 ? `...${str.substring(i + 1)}` : str;
    }
  }
  return str;
}

function cutTrailingWords(str: string) {
  let words = 0;
  for (let i = 0; i < str.length; i++) {
    if (/\s/.test(str[i])) {
      words++;
    }
    if (words === WORDS) {
      return i < str.length - 1 ? `${str.substring(0, i)}...` : str;
    }
  }
  return str;
}

export function cutWords(tokens: Array<{ text: string; marked: boolean }>) {
  const result = [];
  let length = 0;

  const highlightPos = tokens.findIndex(token => token.marked);
  if (highlightPos > 0) {
    const text = cutLeadingWords(tokens[highlightPos - 1].text);
    result.push({ text, marked: false });
    length += text.length;
  }

  result.push(tokens[highlightPos]);
  length += tokens[highlightPos].text.length;

  for (let i = highlightPos + 1; i < tokens.length; i++) {
    if (length + tokens[i].text.length > 100) {
      const text = cutTrailingWords(tokens[i].text);
      result.push({ text, marked: false });
      return result;
    } else {
      result.push(tokens[i]);
      length += tokens[i].text.length;
    }
  }

  return result;
}

export function getMarkdownRemarkTitle(node?: MarkdownRemark) {
  return node && node.frontmatter && (node.frontmatter.nav || node.frontmatter.title);
}

export function getMarkdownRemarkUrl(node?: MarkdownRemark) {
  return (
    (node && node.frontmatter && node.frontmatter.url) || (node && node.fields && node.fields.slug)
  );
}

export function highlightMarks(str: string, marks: Array<{ from: number; to: number }>) {
  const sortedMarks = sortBy(
    [
      ...marks.map(mark => ({ pos: mark.from, start: true })),
      ...marks.map(mark => ({ pos: mark.to, start: false }))
    ],
    mark => mark.pos,
    mark => Number(!mark.start)
  );

  const cuts = [];
  let start = 0;
  let balance = 0;

  for (const mark of sortedMarks) {
    if (mark.start) {
      if (balance === 0 && start !== mark.pos) {
        cuts.push({ text: str.substring(start, mark.pos), marked: false });
        start = mark.pos;
      }
      balance++;
    } else {
      balance--;
      if (balance === 0 && start !== mark.pos) {
        cuts.push({ text: str.substring(start, mark.pos), marked: true });
        start = mark.pos;
      }
    }
  }

  if (start < str.length - 1) {
    cuts.push({ text: str.substr(start), marked: false });
  }

  return cuts;
}

export function isDefined<T>(x: T | undefined | null): x is T {
  return x !== undefined && x !== null;
}
