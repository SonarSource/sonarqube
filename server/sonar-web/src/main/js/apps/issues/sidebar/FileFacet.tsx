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
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { collapsePath } from 'sonar-ui-common/helpers/path';
import { highlightTerm } from 'sonar-ui-common/helpers/search';
import { isDefined } from 'sonar-ui-common/helpers/types';
import { getFiles, TreeComponentWithPath } from '../../../api/components';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { Facet, Query, ReferencedComponent } from '../utils';

interface Props {
  componentKey: string;
  fetching: boolean;
  fileUuids: string[];
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  referencedComponents: T.Dict<ReferencedComponent>;
  stats: Facet | undefined;
}

export default class FileFacet extends React.PureComponent<Props> {
  getFilePath = (fileUuid: string) => {
    const { referencedComponents } = this.props;
    return referencedComponents[fileUuid]
      ? collapsePath(referencedComponents[fileUuid].path || '', 15)
      : fileUuid;
  };

  getReferencedComponent = (key: string) => {
    const { referencedComponents } = this.props;
    const fileUuid = Object.keys(referencedComponents).find(uuid => {
      return referencedComponents[uuid].key === key;
    });
    return fileUuid ? referencedComponents[fileUuid] : undefined;
  };

  getFacetItemText = (fileUuid: string) => {
    const { referencedComponents } = this.props;
    return referencedComponents[fileUuid] ? referencedComponents[fileUuid].path || '' : fileUuid;
  };

  getSearchResultKey = (file: TreeComponentWithPath) => {
    const component = this.getReferencedComponent(file.key);
    return component ? component.uuid : file.key;
  };

  getSearchResultText = (file: TreeComponentWithPath) => {
    return file.path;
  };

  handleSearch = (query: string, page: number) => {
    return getFiles({
      component: this.props.componentKey,
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
          const component = this.getReferencedComponent(file.key);
          return component && component.uuid;
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

  renderFacetItem = (fileUuid: string) => {
    const name = this.getFilePath(fileUuid);
    return this.renderFile(name);
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
        values={this.props.fileUuids}
      />
    );
  }
}
