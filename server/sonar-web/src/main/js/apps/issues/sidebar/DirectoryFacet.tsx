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
import * as React from 'react';
import { omit } from 'lodash';
import { Query, Facet } from '../utils';
import QualifierIcon from '../../../components/icons-components/QualifierIcon';
import { translate } from '../../../helpers/l10n';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { highlightTerm } from '../../../helpers/search';
import { getTree, TreeComponent } from '../../../api/components';
import { collapsePath } from '../../../helpers/path';

interface Props {
  componentKey: string;
  fetching: boolean;
  directories: string[];
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  stats: T.Dict<number> | undefined;
}

export default class DirectoryFacet extends React.PureComponent<Props> {
  getFacetItemText = (directory: string) => {
    return collapsePath(directory, 15);
  };

  getSearchResultKey = (directory: TreeComponent) => {
    return directory.name;
  };

  getSearchResultText = (directory: TreeComponent) => {
    return directory.name;
  };

  handleSearch = (query: string, page: number) => {
    return getTree({
      component: this.props.componentKey,
      q: query,
      qualifiers: 'DIR',
      p: page,
      ps: 30
    }).then(({ components, paging }) => ({ paging, results: components }));
  };

  loadSearchResultCount = (directories: TreeComponent[]) => {
    return this.props.loadSearchResultCount('directories', {
      directories: directories.map(directory => directory.name)
    });
  };

  renderDirectory = (directory: React.ReactNode) => (
    <>
      <QualifierIcon className="little-spacer-right" qualifier="DIR" />
      {directory}
    </>
  );

  renderFacetItem = (directory: string) => {
    return this.renderDirectory(collapsePath(directory, 15));
  };

  renderSearchResult = (directory: TreeComponent, term: string) => {
    return this.renderDirectory(highlightTerm(collapsePath(directory.name), term));
  };

  render() {
    return (
      <ListStyleFacet<TreeComponent>
        facetHeader={translate('issues.facet.directories')}
        fetching={this.props.fetching}
        getFacetItemText={this.getFacetItemText}
        getSearchResultKey={this.getSearchResultKey}
        getSearchResultText={this.getSearchResultText}
        loadSearchResultCount={this.loadSearchResultCount}
        minSearchLength={3}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="directories"
        query={omit(this.props.query, 'directories')}
        renderFacetItem={this.renderFacetItem}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_directories')}
        stats={this.props.stats}
        values={this.props.directories}
      />
    );
  }
}
