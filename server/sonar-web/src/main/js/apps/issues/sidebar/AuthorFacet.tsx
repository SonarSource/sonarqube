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
import { omit } from 'lodash';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { highlightTerm } from 'sonar-ui-common/helpers/search';
import { searchIssueAuthors } from '../../../api/issues';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { Facet, Query } from '../utils';

interface Props {
  component: T.Component | undefined;
  fetching: boolean;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  organization: string | undefined;
  query: Query;
  stats: T.Dict<number> | undefined;
  authors: string[];
}

const SEARCH_SIZE = 100;

export default class AuthorFacet extends React.PureComponent<Props> {
  identity = (author: string) => {
    return author;
  };

  handleSearch = (query: string, _page: number) => {
    const { component } = this.props;
    const project =
      component && ['TRK', 'VW', 'APP'].includes(component.qualifier) ? component.key : undefined;
    return searchIssueAuthors({
      organization: this.props.organization,
      project,
      ps: SEARCH_SIZE, // maximum
      q: query
    }).then(authors => ({ maxResults: authors.length === SEARCH_SIZE, results: authors }));
  };

  loadSearchResultCount = (authors: string[]) => {
    return this.props.loadSearchResultCount('authors', { authors });
  };

  renderSearchResult = (author: string, term: string) => {
    return highlightTerm(author, term);
  };

  render() {
    return (
      <ListStyleFacet<string>
        facetHeader={translate('issues.facet.authors')}
        fetching={this.props.fetching}
        getFacetItemText={this.identity}
        getSearchResultKey={this.identity}
        getSearchResultText={this.identity}
        loadSearchResultCount={this.loadSearchResultCount}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="authors"
        query={omit(this.props.query, 'authors')}
        renderFacetItem={this.identity}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_authors')}
        stats={this.props.stats}
        values={this.props.authors}
      />
    );
  }
}
