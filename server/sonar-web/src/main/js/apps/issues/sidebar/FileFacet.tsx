/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { collapsePath } from 'sonar-ui-common/helpers/path';
import { highlightTerm } from 'sonar-ui-common/helpers/search';
import { isDefined } from 'sonar-ui-common/helpers/types';
import { getFiles } from '../../../api/components';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import { TreeComponentWithPath } from '../../../types/component';
import { Facet, Query } from '../utils';

interface Props {
  branchLike?: BranchLike;
  componentKey: string;
  fetching: boolean;
  files: string[];
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  stats: Facet | undefined;
}

export default class FileFacet extends React.PureComponent<Props> {
  getFacetItemText = (path: string) => {
    return path;
  };

  getSearchResultKey = (file: TreeComponentWithPath) => {
    return file.path;
  };

  getSearchResultText = (file: TreeComponentWithPath) => {
    return file.path;
  };

  handleSearch = (query: string, page: number) => {
    const { branchLike } = this.props;

    return getFiles({
      component: this.props.componentKey,
      ...getBranchLikeQuery(branchLike),
      q: query,
      p: page,
      ps: 30
    }).then(({ components, paging }) => ({
      paging,
      results: components.filter(file => file.path !== undefined)
    }));
  };

  loadSearchResultCount = (files: TreeComponentWithPath[]) => {
    return this.props.loadSearchResultCount('files', {
      files: files
        .map(file => {
          return file.path;
        })
        .filter(isDefined)
    });
  };

  renderFile = (file: React.ReactNode) => (
    <>
      <QualifierIcon className="little-spacer-right" qualifier="FIL" />
      {file}
    </>
  );

  renderFacetItem = (path: string) => {
    return this.renderFile(path);
  };

  renderSearchResult = (file: TreeComponentWithPath, term: string) => {
    return this.renderFile(highlightTerm(collapsePath(file.path, 15), term));
  };

  render() {
    return (
      <ListStyleFacet<TreeComponentWithPath>
        facetHeader={translate('issues.facet.files')}
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
        property="files"
        query={omit(this.props.query, 'files')}
        renderFacetItem={this.renderFacetItem}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_files')}
        stats={this.props.stats}
        values={this.props.files}
      />
    );
  }
}
