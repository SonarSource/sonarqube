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
import { QualifierIcon } from 'design-system';
import { omit } from 'lodash';
import * as React from 'react';
import { getDirectories } from '../../../api/components';
import { translate } from '../../../helpers/l10n';
import { collapsePath } from '../../../helpers/path';
import { highlightTerm } from '../../../helpers/search';
import { getBranchLikeQuery } from '../../../sonar-aligned/helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import { TreeComponentWithPath } from '../../../types/component';
import { Facet } from '../../../types/issues';
import { MetricKey } from '../../../types/metrics';
import { Query } from '../utils';
import { ListStyleFacet } from './ListStyleFacet';

interface Props {
  branchLike?: BranchLike;
  componentKey: string;
  directories: string[];
  fetching: boolean;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  stats: Facet | undefined;
}

export class DirectoryFacet extends React.PureComponent<Props> {
  getFacetItemText = (path: string) => {
    return path;
  };

  getSearchResultKey = (directory: TreeComponentWithPath) => {
    return directory.path;
  };

  getSearchResultText = (directory: TreeComponentWithPath) => {
    return directory.path;
  };

  handleSearch = (query: string, page: number) => {
    const { branchLike } = this.props;

    return getDirectories({
      component: this.props.componentKey,
      ...getBranchLikeQuery(branchLike),
      q: query,
      p: page,
      ps: 30,
    }).then(({ components, paging }) => ({
      paging,
      results: components.filter((dir) => dir.path !== undefined),
    }));
  };

  loadSearchResultCount = (directories: TreeComponentWithPath[]) => {
    return this.props.loadSearchResultCount(MetricKey.directories, {
      directories: directories.map((directory) => directory.path),
    });
  };

  renderDirectory = (directory: React.ReactNode) => (
    <>
      <QualifierIcon qualifier="dir" className="sw-mr-1" />

      {directory}
    </>
  );

  renderFacetItem = (path: string) => {
    return this.renderDirectory(collapsePath(path, 15));
  };

  renderSearchResult = (directory: TreeComponentWithPath, term: string) => {
    return this.renderDirectory(highlightTerm(collapsePath(directory.path, 15), term));
  };

  render() {
    return (
      <ListStyleFacet<TreeComponentWithPath>
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
        property={MetricKey.directories}
        query={omit(this.props.query, MetricKey.directories)}
        renderFacetItem={this.renderFacetItem}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_directories')}
        stats={this.props.stats}
        values={this.props.directories}
      />
    );
  }
}
