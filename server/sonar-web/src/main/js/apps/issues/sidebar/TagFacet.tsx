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
import { searchIssueTags } from '../../../api/issues';
import { colors } from '../../../app/theme';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import TagsIcon from '../../../components/icons/TagsIcon';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';
import { Facet } from '../../../types/issues';
import { Component, Dict } from '../../../types/types';
import { Query } from '../utils';

interface Props {
  component: Component | undefined;
  branch?: string;
  fetching: boolean;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  stats: Dict<number> | undefined;
  tags: string[];
}

const SEARCH_SIZE = 100;

export default class TagFacet extends React.PureComponent<Props> {
  handleSearch = (query: string) => {
    const { component, branch } = this.props;
    const project =
      component && ['TRK', 'VW', 'APP'].includes(component.qualifier) ? component.key : undefined;
    return searchIssueTags({
      project,
      branch,
      ps: SEARCH_SIZE,
      q: query,
    }).then((tags) => ({ maxResults: tags.length === SEARCH_SIZE, results: tags }));
  };

  getTagName = (tag: string) => {
    return tag;
  };

  loadSearchResultCount = (tags: string[]) => {
    return this.props.loadSearchResultCount('tags', { tags });
  };

  renderTag = (tag: string) => {
    return (
      <>
        <TagsIcon className="little-spacer-right" fill={colors.gray60} />
        {tag}
      </>
    );
  };

  renderSearchResult = (tag: string, term: string) => (
    <>
      <TagsIcon className="little-spacer-right" fill={colors.gray60} />
      {highlightTerm(tag, term)}
    </>
  );

  render() {
    return (
      <ListStyleFacet<string>
        facetHeader={translate('issues.facet.tags')}
        fetching={this.props.fetching}
        getFacetItemText={this.getTagName}
        getSearchResultKey={(tag) => tag}
        getSearchResultText={(tag) => tag}
        loadSearchResultCount={this.loadSearchResultCount}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="tags"
        query={omit(this.props.query, 'tags')}
        renderFacetItem={this.renderTag}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_tags')}
        stats={this.props.stats}
        values={this.props.tags}
      />
    );
  }
}
