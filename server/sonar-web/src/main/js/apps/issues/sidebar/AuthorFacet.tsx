/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { omit } from 'lodash';
import * as React from 'react';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { searchIssueAuthors } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';
import { Facet } from '../../../types/issues';
import { Component, Dict } from '../../../types/types';
import { Query } from '../utils';
import { ListStyleFacet } from './ListStyleFacet';

interface Props {
  author: string[];
  component: Component | undefined;
  fetching: boolean;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  stats: Dict<number> | undefined;
}

const SEARCH_SIZE = 100;

export class AuthorFacet extends React.PureComponent<Props> {
  handleSearch = (query: string, _page: number) => {
    const { component } = this.props;

    const project =
      component &&
      [
        ComponentQualifier.Application,
        ComponentQualifier.Portfolio,
        ComponentQualifier.Project,
      ].includes(component.qualifier as ComponentQualifier)
        ? component.key
        : undefined;

    return searchIssueAuthors({
      project,
      ps: SEARCH_SIZE, // maximum
      q: query,
    }).then((authors) => ({ maxResults: authors.length === SEARCH_SIZE, results: authors }));
  };

  loadSearchResultCount = (author: string[]) => {
    return this.props.loadSearchResultCount('author', { author });
  };

  renderSearchResult = (author: string, term: string) => {
    return highlightTerm(author, term);
  };

  render() {
    return (
      <ListStyleFacet<string>
        facetHeader={translate('issues.facet.authors')}
        fetching={this.props.fetching}
        loadSearchResultCount={this.loadSearchResultCount}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="author"
        query={omit(this.props.query, 'author')}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_authors')}
        stats={this.props.stats}
        values={this.props.author}
      />
    );
  }
}
