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
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { Query, ReferencedRule } from '../utils';
import { searchRules } from '../../../api/rules';
import { Rule, Paging } from '../../../app/types';
import { translate } from '../../../helpers/l10n';

interface Props {
  fetching: boolean;
  languages: string[];
  loading?: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  organization: string | undefined;
  referencedRules: { [ruleKey: string]: ReferencedRule };
  rules: string[];
  stats: { [x: string]: number } | undefined;
}

interface State {
  query: string;
  searching: boolean;
  searchResults?: Rule[];
  searchPaging?: Paging;
}

export default class RuleFacet extends React.PureComponent<Props, State> {
  handleSearch = (query: string, page = 1) => {
    const { languages, organization } = this.props;
    return searchRules({
      f: 'name,langName',
      languages: languages.length ? languages.join() : undefined,
      organization,
      q: query,
      p: page,
      ps: 30,
      s: 'name',
      // eslint-disable-next-line camelcase
      include_external: true
    }).then(response => ({
      paging: { pageIndex: response.p, pageSize: response.ps, total: response.total },
      results: response.rules
    }));
  };

  getRuleName = (rule: string) => {
    const { referencedRules } = this.props;
    return referencedRules[rule]
      ? `(${referencedRules[rule].langName}) ${referencedRules[rule].name}`
      : rule;
  };

  renderSearchResult = (rule: Rule) => {
    return `(${rule.langName}) ${rule.name}`;
  };

  render() {
    return (
      <ListStyleFacet
        facetHeader={translate('issues.facet.rules')}
        fetching={this.props.fetching}
        getFacetItemText={this.getRuleName}
        getSearchResultKey={result => result.key}
        getSearchResultText={result => result.name}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="rules"
        renderFacetItem={this.getRuleName}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_rules')}
        stats={this.props.stats}
        values={this.props.rules}
      />
    );
  }
}
