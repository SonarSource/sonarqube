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
import { Query, ReferencedComponent, Facet } from '../utils';
import QualifierIcon from '../../../components/icons-components/QualifierIcon';
import { translate } from '../../../helpers/l10n';
import { collapsePath } from '../../../helpers/path';
import { TreeComponent, getTree } from '../../../api/components';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { highlightTerm } from '../../../helpers/search';

interface Props {
  componentKey: string;
  fetching: boolean;
  files: string[];
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  referencedComponents: T.Dict<ReferencedComponent>;
  stats: T.Dict<number> | undefined;
}

export default class FileFacet extends React.PureComponent<Props> {
  getFile = (file: string) => {
    const { referencedComponents } = this.props;
    return referencedComponents[file]
      ? collapsePath(referencedComponents[file].path || '', 15)
      : file;
  };

  getFacetItemText = (file: string) => {
    const { referencedComponents } = this.props;
    return referencedComponents[file] ? referencedComponents[file].path || '' : file;
  };

  getSearchResultKey = (file: TreeComponent) => {
    return file.id;
  };

  getSearchResultText = (file: TreeComponent) => {
    return file.path || file.name;
  };

  handleSearch = (query: string, page: number) => {
    return getTree({
      component: this.props.componentKey,
      q: query,
      qualifiers: 'FIL',
      p: page,
      ps: 30
    }).then(({ components, paging }) => ({ paging, results: components }));
  };

  loadSearchResultCount = (files: TreeComponent[]) => {
    return this.props.loadSearchResultCount('files', { files: files.map(file => file.id) });
  };

  renderFile = (file: React.ReactNode) => (
    <>
      <QualifierIcon className="little-spacer-right" qualifier="FIL" />
      {file}
    </>
  );

  renderFacetItem = (file: string) => {
    const name = this.getFile(file);
    return this.renderFile(name);
  };

  renderSearchResult = (file: TreeComponent, term: string) => {
    return this.renderFile(highlightTerm(collapsePath(file.path || file.name, 15), term));
  };

  render() {
    return (
      <ListStyleFacet<TreeComponent>
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
