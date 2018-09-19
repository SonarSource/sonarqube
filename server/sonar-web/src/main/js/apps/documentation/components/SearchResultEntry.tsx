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
import * as classNames from 'classnames';
import { Link } from 'react-router';
import { highlightMarks, cutWords, DocumentationEntry } from '../utils';

export interface SearchResult {
  highlights: { [field: string]: [number, number][] };
  longestTerm: string;
  page: DocumentationEntry;
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
