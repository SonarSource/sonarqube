/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { cloneDeep, countBy } from 'lodash';
import { mockQualityProfile, mockRule, mockRuleRepository } from '../../helpers/testMocks';
import { RuleRepository } from '../../types/coding-rules';
import { SearchRulesQuery } from '../../types/rules';
import { Rule } from '../../types/types';
import {
  bulkActivateRules,
  bulkDeactivateRules,
  Profile,
  searchQualityProfiles,
  SearchQualityProfilesParameters,
  SearchQualityProfilesResponse
} from '../quality-profiles';
import { getRulesApp, searchRules } from '../rules';

interface FacetFilter {
  languages?: string;
}

const FACET_RULE_MAP: { [key: string]: keyof Rule } = {
  languages: 'lang',
  types: 'type'
};
export default class CodingRulesMock {
  defaultRules: Rule[] = [];
  rules: Rule[] = [];
  qualityProfile: Profile[] = [];
  repositories: RuleRepository[] = [];
  isAdmin = false;
  applyWithWarning = false;

  constructor() {
    this.repositories = [
      mockRuleRepository({ key: 'repo1' }),
      mockRuleRepository({ key: 'repo2' })
    ];
    this.qualityProfile = [
      mockQualityProfile({ key: 'p1', name: 'QP Foo', language: 'java', languageName: 'Java' }),
      mockQualityProfile({ key: 'p2', name: 'QP Bar', language: 'js' }),
      mockQualityProfile({ key: 'p3', name: 'QP FooBar', language: 'java', languageName: 'Java' })
    ];

    this.defaultRules = [
      mockRule({
        key: 'rule1',
        type: 'BUG',
        lang: 'java',
        langName: 'Java',
        name: 'Awsome java rule'
      }),
      mockRule({ key: 'rule2', name: 'Hot hotspot', type: 'SECURITY_HOTSPOT' }),
      mockRule({ key: 'rule3', name: 'Unknown rule' }),
      mockRule({ key: 'rule4', type: 'BUG', lang: 'c', langName: 'C', name: 'Awsome C rule' })
    ];

    (searchRules as jest.Mock).mockImplementation(this.handleSearchRules);
    (searchQualityProfiles as jest.Mock).mockImplementation(this.handleSearchQualityProfiles);
    (getRulesApp as jest.Mock).mockImplementation(this.handleGetRulesApp);
    (bulkActivateRules as jest.Mock).mockImplementation(this.handleBulkActivateRules);
    (bulkDeactivateRules as jest.Mock).mockImplementation(this.handleBulkDeactivateRules);

    this.rules = cloneDeep(this.defaultRules);
  }

  filterFacet({ languages }: FacetFilter) {
    let filteredRules = this.rules;
    if (languages) {
      filteredRules = filteredRules.filter(r => r.lang && languages.includes(r.lang));
    }
    return filteredRules;
  }

  setIsAdmin() {
    this.isAdmin = true;
  }

  activateWithWarning() {
    this.applyWithWarning = true;
  }

  reset() {
    this.isAdmin = false;
    this.applyWithWarning = false;
    this.rules = cloneDeep(this.defaultRules);
  }

  allRulesName() {
    return this.rules.map(r => r.name);
  }

  allQualityProfile(language: string) {
    return this.qualityProfile.filter(qp => qp.language === language);
  }

  handleSearchRules = ({ facets, languages, p, ps }: SearchRulesQuery) => {
    const countFacet = (facets || '').split(',').map((facet: keyof Rule) => {
      const facetCount = countBy(this.rules.map(r => r[FACET_RULE_MAP[facet] || facet] as string));
      return {
        property: facet,
        values: Object.keys(facetCount).map(val => ({ val, count: facetCount[val] }))
      };
    });
    const currentPs = ps || 10;
    const currentP = p || 1;
    const filteredRules = this.filterFacet({ languages });
    const responseRules = filteredRules.slice((currentP - 1) * currentPs, currentP * currentPs);
    return this.reply({
      total: filteredRules.length,
      p: currentP,
      ps: currentPs,
      rules: responseRules,
      facets: countFacet
    });
  };

  handleBulkActivateRules = () => {
    if (this.applyWithWarning) {
      return this.reply({
        succeeded: this.rules.length - 1,
        failed: 1,
        errors: [{ msg: 'c rule c:S6069 cannot be activated on cpp profile SonarSource' }]
      });
    }
    return this.reply({
      succeeded: this.rules.length,
      failed: 0,
      errors: []
    });
  };

  handleBulkDeactivateRules = () => {
    return this.reply({
      succeeded: this.rules.length,
      failed: 0
    });
  };

  handleSearchQualityProfiles = ({ language }: SearchQualityProfilesParameters = {}): Promise<
    SearchQualityProfilesResponse
  > => {
    let profiles: Profile[] = this.isAdmin
      ? this.qualityProfile.map(p => ({ ...p, actions: { edit: true } }))
      : this.qualityProfile;
    if (language) {
      profiles = profiles.filter(p => p.language === language);
    }
    return this.reply({ profiles });
  };

  handleGetRulesApp = () => {
    return this.reply({ canWrite: this.isAdmin, repositories: this.repositories });
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
