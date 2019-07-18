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
import * as classNames from 'classnames';
import * as React from 'react';
import { Link } from 'react-router';
import { cutWords, DocumentationEntry, highlightMarks } from '../utils';

export interface SearchResult {
  exactMatch?: boolean;
  highlights: T.Dict<[number, number][]>;
  longestTerm: string;
  page: DocumentationEntry;
  query: string;
}

interface Props {
  active: boolean;
  result: SearchResult;
}

export default function SearchResultEntry({ active, result }: Props) {
  return (
    <Link
      className={classNames('list-group-item', { active })}
      to={'/documentation' + result.page.url}>
      <SearchResultTitle result={result} />
      <SearchResultText result={result} />
    </Link>
  );
}

export function SearchResultTitle({ result }: { result: SearchResult }) {
  let titleWithMarks: React.ReactNode;

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

  return (
    <h3 className="list-group-item-heading" style={{ fontWeight: 'normal' }}>
      {titleWithMarks}
    </h3>
  );
}

export function SearchResultText({ result }: { result: SearchResult }) {
  const textHighlights = result.highlights.text;
  const { text } = result.page;
  let tokens: {
    text: string;
    marked: boolean;
  }[] = [];

  if (result.exactMatch) {
    const pageText = result.page.text.toLowerCase();
    const highlights: { from: number; to: number }[] = [];
    let start = 0;
    let index = pageText.indexOf(result.query, start);
    let loopCount = 0;

    while (index > -1 && loopCount < 10) {
      loopCount++;
      highlights.push({ from: index, to: index + result.query.length });
      start = index + 1;
      index = pageText.indexOf(result.query, start);
    }

    if (highlights.length) {
      tokens = highlightMarks(text, highlights);
    }
  }

  if (tokens.length === 0 && textHighlights && textHighlights.length > 0) {
    tokens = highlightMarks(text, textHighlights.map(h => ({ from: h[0], to: h[0] + h[1] })));
  }

  if (tokens.length) {
    return (
      <div className="note">
        <SearchResultTokens tokens={cutWords(tokens)} />
      </div>
    );
  } else {
    return null;
  }
}

export function SearchResultTokens({
  tokens
}: {
  tokens: Array<{ text: string; marked: boolean }>;
}) {
  return (
    <>
      {tokens.map((token, index) => (
        <React.Fragment key={index}>
          {token.marked ? <mark key={index}>{token.text}</mark> : token.text}
        </React.Fragment>
      ))}
    </>
  );
}
