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
import { Query } from '../utils';
import { searchIssueTags } from '../../../api/issues';
import * as theme from '../../../app/theme';
import { Component } from '../../../app/types';
import TagsIcon from '../../../components/icons-components/TagsIcon';
import { translate } from '../../../helpers/l10n';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { highlightTerm } from '../../../helpers/search';

interface Props {
  component: Component | undefined;
  fetching: boolean;
  loading?: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  organization: string | undefined;
  stats: { [x: string]: number } | undefined;
  tags: string[];
}

export default class TagFacet extends React.PureComponent<Props> {
  handleSearch = (query: string) => {
    return searchIssueTags({ organization: this.props.organization, ps: 50, q: query }).then(
      tags => ({
        paging: { pageIndex: 1, pageSize: tags.length, total: tags.length },
        results: tags
      })
    );
  };

  getTagName = (tag: string) => {
    return tag;
  };

  renderTag = (tag: string) => {
    return (
      <>
        <TagsIcon className="little-spacer-right" fill={theme.gray60} />
        {tag}
      </>
    );
  };

  renderSearchResult = (tag: string, term: string) => (
    <>
      <TagsIcon className="little-spacer-right" fill={theme.gray60} />
      {highlightTerm(tag, term)}
    </>
  );

  render() {
    return (
      <ListStyleFacet
        facetHeader={translate('issues.facet.tags')}
        fetching={this.props.fetching}
        getFacetItemText={this.getTagName}
        getSearchResultKey={tag => tag}
        getSearchResultText={tag => tag}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="tags"
        renderFacetItem={this.renderTag}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_tags')}
        stats={this.props.stats}
        values={this.props.tags}
      />
    );
  }
}
