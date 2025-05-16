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
import { searchRules } from '../../../api/rules';
import { ISSUE_TYPES } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { Facet, IssueType, ReferencedRule } from '../../../types/issues';
import { Dict, Rule } from '../../../types/types';
import { Query } from '../utils';
import { ListStyleFacet } from './ListStyleFacet';

interface Props {
  organization: string;
  fetching: boolean;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  referencedRules: Dict<ReferencedRule>;
  stats: Dict<number> | undefined;
}

export class RuleFacet extends React.PureComponent<Props> {
  handleSearch = (query: string, page = 1) => {
    const { organization, languages, types } = this.props.query;

    return searchRules({
      organization: this.props.organization,
      f: 'name,langName',
      languages: languages.length ? languages.join() : undefined,
      q: query,
      p: page,
      ps: 30,
      types: types.length
        ? types.join()
        : ISSUE_TYPES.filter((type) => type !== IssueType.SecurityHotspot).join(),
      s: 'name',
      include_external: true,
    }).then(({ rules, paging }) => ({
      results: rules,
      paging,
    }));
  };

  loadSearchResultCount = (rules: Rule[]) => {
    return this.props.loadSearchResultCount('rules', { rules: rules.map((rule) => rule.key) });
  };

  getRuleName = (ruleKey: string) => {
    const rule = this.props.referencedRules[ruleKey];

    return rule ? this.formatRuleName(rule.name, rule.langName) : ruleKey;
  };

  formatRuleName = (name: string, langName: string | undefined) => {
    // external rules don't have a language associated
    // see https://jira.sonarsource.com/browse/MMF-1407
    return langName ? `(${langName}) ${name}` : name;
  };

  renderSearchResult = (rule: Rule) => {
    return this.formatRuleName(rule.name, rule.langName);
  };

  render() {
    const { fetching, open, query, stats } = this.props;

    return (
      <ListStyleFacet<Rule>
        facetHeader={translate('issues.facet.rules')}
        fetching={fetching}
        getFacetItemText={this.getRuleName}
        getSearchResultKey={(rule) => rule.key}
        getSearchResultText={(rule) => rule.name}
        loadSearchResultCount={this.loadSearchResultCount}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={open}
        property="rules"
        query={omit(query, 'rules')}
        renderFacetItem={this.getRuleName}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_rules')}
        stats={stats}
        values={query.rules}
      />
    );
  }
}
