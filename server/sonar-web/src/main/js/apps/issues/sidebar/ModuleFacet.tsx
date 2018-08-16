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
import { omit } from 'lodash';
import { Query, ReferencedComponent, Facet } from '../utils';
import QualifierIcon from '../../../components/icons-components/QualifierIcon';
import { translate } from '../../../helpers/l10n';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { TreeComponent, getTree } from '../../../api/components';
import { highlightTerm } from '../../../helpers/search';

interface Props {
  componentKey: string;
  fetching: boolean;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  modules: string[];
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  referencedComponents: { [componentKey: string]: ReferencedComponent };
  stats: { [x: string]: number } | undefined;
}

export default class ModuleFacet extends React.PureComponent<Props> {
  getModuleName = (module: string) => {
    const { referencedComponents } = this.props;
    return referencedComponents[module] ? referencedComponents[module].name : module;
  };

  getSearchResultKey = (module: TreeComponent) => {
    return module.id;
  };

  getSearchResultText = (module: TreeComponent) => {
    return module.name;
  };

  handleSearch = (query: string, page: number) => {
    return getTree({
      component: this.props.componentKey,
      q: query,
      qualifiers: 'BRC',
      p: page,
      ps: 30
    }).then(({ components, paging }) => ({ paging, results: components }));
  };

  loadSearchResultCount = (modules: TreeComponent[]) => {
    return this.props.loadSearchResultCount('modules', {
      modules: modules.map(module => module.id)
    });
  };

  renderModule = (module: React.ReactNode) => (
    <>
      <QualifierIcon className="little-spacer-right" qualifier="BRC" />
      {module}
    </>
  );

  renderFacetItem = (module: string) => {
    const name = this.getModuleName(module);
    return this.renderModule(name);
  };

  renderSearchResult = (module: TreeComponent, term: string) => {
    return this.renderModule(highlightTerm(module.name, term));
  };

  render() {
    return (
      <ListStyleFacet<TreeComponent>
        facetHeader={translate('issues.facet.modules')}
        fetching={this.props.fetching}
        getFacetItemText={this.getModuleName}
        getSearchResultKey={this.getSearchResultKey}
        getSearchResultText={this.getSearchResultText}
        loadSearchResultCount={this.loadSearchResultCount}
        minSearchLength={3}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="modules"
        query={omit(this.props.query, 'modules')}
        renderFacetItem={this.renderFacetItem}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_modules')}
        stats={this.props.stats}
        values={this.props.modules}
      />
    );
  }
}
