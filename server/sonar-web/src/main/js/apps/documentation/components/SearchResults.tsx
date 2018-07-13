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
import lunr, { LunrIndex } from 'lunr';
import SearchResultEntry, { SearchResult } from './SearchResultEntry';
import { DocumentationEntry } from '../utils';

interface Props {
  pages: DocumentationEntry[];
  query: string;
  splat: string;
}

export default class SearchResults extends React.PureComponent<Props> {
  index: LunrIndex;

  constructor(props: Props) {
    super(props);
    this.index = lunr(function() {
      this.ref('relativeName');
      this.field('title', { boost: 10 });
      this.field('text');

      this.metadataWhitelist = ['position'];

      props.pages.forEach(page => this.add(page));
    });
  }

  render() {
    const { query } = this.props;
    const results = this.index
      .search(`${query}~1 ${query}*`)
      .map(match => {
        const page = this.props.pages.find(page => page.relativeName === match.ref);
        const highlights: { [field: string]: [number, number][] } = {};

        Object.keys(match.matchData.metadata).forEach(term => {
          Object.keys(match.matchData.metadata[term]).forEach(fieldName => {
            const { position: positions } = match.matchData.metadata[term][fieldName];
            highlights[fieldName] = [...(highlights[fieldName] || []), ...positions];
          });
        });

        return { page, highlights };
      })
      .filter(result => result.page) as SearchResult[];

    return (
      <>
        {results.map(result => (
          <SearchResultEntry
            active={result.page.relativeName === this.props.splat}
            key={result.page.relativeName}
            result={result}
          />
        ))}
      </>
    );
  }
}
