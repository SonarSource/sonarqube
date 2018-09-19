/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import Link from 'gatsby-link';
import { highlightMarks, cutWords } from '../utils';

export default function SearchResultEntry({ active, result }) {
  return (
    <Link className={active ? 'active search-result' : 'search-result'} to={result.page.url}>
      <SearchResultTitle result={result} />
      <SearchResultText result={result} />
    </Link>
  );
}

export function SearchResultTitle({ result }) {
  let titleWithMarks;

  const titleHighlights = result.highlights.title;
  if (titleHighlights && titleHighlights.length > 0) {
    const { title } = result.page;
    const tokens = highlightMarks(
      title,
      titleHighlights.map(h => ({ from: h[0], to: h[0] + h[1] }))
    );
    titleWithMarks = <SearchResultTokens tokens={tokens} />;
  } else {
    titleWithMarks = result.page.title;
  }

  return <div className="search-result">{titleWithMarks}</div>;
}

export function SearchResultText({ result }) {
  const textHighlights = result.highlights.text;
  if (textHighlights && textHighlights.length > 0) {
    const { text } = result.page;
    const tokens = highlightMarks(text, textHighlights.map(h => ({ from: h[0], to: h[0] + h[1] })));
    return (
      <div className="note">
        <SearchResultTokens tokens={cutWords(tokens)} />
      </div>
    );
  } else {
    return null;
  }
}

export function SearchResultTokens({ tokens }) {
  return (
    <span>
      {tokens.map((token, index) => (
        <span key={index}>{token.marked ? <mark key={index}>{token.text}</mark> : token.text}</span>
      ))}
    </span>
  );
}
