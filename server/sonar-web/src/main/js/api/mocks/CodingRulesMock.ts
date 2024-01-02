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
import { cloneDeep, countBy, pick, trim } from 'lodash';
import { RuleDescriptionSections } from '../../apps/coding-rules/rule';
import {
  mockCurrentUser,
  mockQualityProfile,
  mockRuleDetails,
  mockRuleRepository,
} from '../../helpers/testMocks';
import { RuleRepository } from '../../types/coding-rules';
import { RawIssuesResponse } from '../../types/issues';
import { SearchRulesQuery } from '../../types/rules';
import { Rule, RuleActivation, RuleDetails, RulesUpdateRequest } from '../../types/types';
import { NoticeType } from '../../types/users';
import { getFacet } from '../issues';
import {
  bulkActivateRules,
  bulkDeactivateRules,
  Profile,
  searchQualityProfiles,
  SearchQualityProfilesParameters,
  SearchQualityProfilesResponse,
} from '../quality-profiles';
import { getRuleDetails, getRulesApp, searchRules, updateRule } from '../rules';
import { dismissNotice, getCurrentUser } from '../users';

interface FacetFilter {
  languages?: string;
}

const FACET_RULE_MAP: { [key: string]: keyof Rule } = {
  languages: 'lang',
  types: 'type',
};
export default class CodingRulesMock {
  defaultRules: RuleDetails[] = [];
  rules: RuleDetails[] = [];
  qualityProfile: Profile[] = [];
  repositories: RuleRepository[] = [];
  isAdmin = false;
  applyWithWarning = false;
  dismissedNoticesEP = false;

  constructor() {
    this.repositories = [
      mockRuleRepository({ key: 'repo1' }),
      mockRuleRepository({ key: 'repo2' }),
    ];
    this.qualityProfile = [
      mockQualityProfile({ key: 'p1', name: 'QP Foo', language: 'java', languageName: 'Java' }),
      mockQualityProfile({ key: 'p2', name: 'QP Bar', language: 'js' }),
      mockQualityProfile({ key: 'p3', name: 'QP FooBar', language: 'java', languageName: 'Java' }),
    ];

    const resourceContent = 'Some link <a href="http://example.com">Awsome Reading</a>';
    const introTitle = 'Introduction to this rule';
    const rootCauseContent = 'This how to fix';

    this.defaultRules = [
      mockRuleDetails({
        key: 'rule1',
        type: 'BUG',
        lang: 'java',
        langName: 'Java',
        name: 'Awsome java rule',
      }),
      mockRuleDetails({
        key: 'rule2',
        name: 'Hot hotspot',
        type: 'SECURITY_HOTSPOT',
        lang: 'js',
        descriptionSections: [
          { key: RuleDescriptionSections.INTRODUCTION, content: introTitle },
          { key: RuleDescriptionSections.ROOT_CAUSE, content: rootCauseContent },
          { key: RuleDescriptionSections.ASSESS_THE_PROBLEM, content: 'Assess' },
          {
            key: RuleDescriptionSections.RESOURCES,
            content: resourceContent,
          },
        ],
        langName: 'JavaScript',
      }),
      mockRuleDetails({ key: 'rule3', name: 'Unknown rule', lang: 'js', langName: 'JavaScript' }),
      mockRuleDetails({
        key: 'rule4',
        type: 'BUG',
        lang: 'c',
        langName: 'C',
        name: 'Awsome C rule',
      }),
      mockRuleDetails({
        key: 'rule5',
        type: 'VULNERABILITY',
        lang: 'py',
        langName: 'Python',
        name: 'Awsome Python rule',
        descriptionSections: [
          { key: RuleDescriptionSections.INTRODUCTION, content: introTitle },
          { key: RuleDescriptionSections.HOW_TO_FIX, content: rootCauseContent },
          {
            key: RuleDescriptionSections.RESOURCES,
            content: resourceContent,
          },
        ],
      }),
      mockRuleDetails({
        key: 'rule6',
        type: 'VULNERABILITY',
        lang: 'py',
        langName: 'Python',
        name: 'Bad Python rule',
        isExternal: true,
        descriptionSections: undefined,
      }),
      mockRuleDetails({
        key: 'rule7',
        type: 'VULNERABILITY',
        lang: 'py',
        langName: 'Python',
        name: 'Python rule with context',
        descriptionSections: [
          {
            key: RuleDescriptionSections.INTRODUCTION,
            content: 'Introduction to this rule with context',
          },
          {
            key: RuleDescriptionSections.HOW_TO_FIX,
            content: 'This is how to fix for spring',
            context: { key: 'spring', displayName: 'Spring' },
          },
          {
            key: RuleDescriptionSections.HOW_TO_FIX,
            content: 'This is how to fix for spring boot',
            context: { key: 'spring_boot', displayName: 'Spring boot' },
          },
          {
            key: RuleDescriptionSections.RESOURCES,
            content: resourceContent,
          },
        ],
      }),
      mockRuleDetails({
        key: 'rule8',
        type: 'VULNERABILITY',
        lang: 'py',
        langName: 'Python',
        name: 'Awesome Python rule with education principles',
        descriptionSections: [
          { key: RuleDescriptionSections.INTRODUCTION, content: introTitle },
          { key: RuleDescriptionSections.HOW_TO_FIX, content: rootCauseContent },
          {
            key: RuleDescriptionSections.RESOURCES,
            content: resourceContent,
          },
        ],
        educationPrinciples: ['defense_in_depth', 'never_trust_user_input'],
      }),
    ];

    (updateRule as jest.Mock).mockImplementation(this.handleUpdateRule);
    (searchRules as jest.Mock).mockImplementation(this.handleSearchRules);
    (getRuleDetails as jest.Mock).mockImplementation(this.handleGetRuleDetails);
    (searchQualityProfiles as jest.Mock).mockImplementation(this.handleSearchQualityProfiles);
    (getRulesApp as jest.Mock).mockImplementation(this.handleGetRulesApp);
    (bulkActivateRules as jest.Mock).mockImplementation(this.handleBulkActivateRules);
    (bulkDeactivateRules as jest.Mock).mockImplementation(this.handleBulkDeactivateRules);
    (getFacet as jest.Mock).mockImplementation(this.handleGetGacet);
    (getCurrentUser as jest.Mock).mockImplementation(this.handleGetCurrentUser);
    (dismissNotice as jest.Mock).mockImplementation(this.handleDismissNotification);
    this.rules = cloneDeep(this.defaultRules);
  }

  getRuleWithoutDetails() {
    return this.rules.map((r) =>
      pick(r, [
        'isTemplate',
        'key',
        'lang',
        'langName',
        'name',
        'params',
        'severity',
        'status',
        'sysTags',
        'tags',
        'type',
      ])
    );
  }

  filterFacet({ languages }: FacetFilter) {
    let filteredRules = this.getRuleWithoutDetails();
    if (languages) {
      filteredRules = filteredRules.filter((r) => r.lang && languages.includes(r.lang));
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
    this.dismissedNoticesEP = false;
    this.rules = cloneDeep(this.defaultRules);
  }

  allRulesCount() {
    return this.rules.length;
  }

  allRulesName() {
    return this.rules.map((r) => r.name);
  }

  allQualityProfile(language: string) {
    return this.qualityProfile.filter((qp) => qp.language === language);
  }

  handleGetGacet = (): Promise<{
    facet: { count: number; val: string }[];
    response: RawIssuesResponse;
  }> => {
    return this.reply({
      facet: [],
      response: {
        components: [],
        effortTotal: 0,
        facets: [],
        issues: [],
        languages: [],
        paging: { total: 0, pageIndex: 1, pageSize: 1 },
      },
    });
  };

  handleGetRuleDetails = (parameters: {
    actives?: boolean;
    key: string;
  }): Promise<{ actives?: RuleActivation[]; rule: RuleDetails }> => {
    const rule = this.rules.find((r) => r.key === parameters.key);
    if (!rule) {
      return Promise.reject({
        errors: [{ msg: `No rule has been found for id ${parameters.key}` }],
      });
    }
    return this.reply({ actives: parameters.actives ? [] : undefined, rule });
  };

  handleUpdateRule = (data: RulesUpdateRequest): Promise<RuleDetails> => {
    const rule = this.rules.find((r) => r.key === data.key);
    if (rule === undefined) {
      return Promise.reject({
        errors: [{ msg: `No rule has been found for id ${data.key}` }],
      });
    }
    const template = this.rules.find((r) => r.key === rule.templateKey);

    // Lets not convert the md to html in test.
    rule.mdDesc = data.markdown_description !== undefined ? data.markdown_description : rule.mdDesc;
    rule.htmlDesc =
      data.markdown_description !== undefined ? data.markdown_description : rule.htmlDesc;
    rule.mdNote = data.markdown_note !== undefined ? data.markdown_note : rule.mdNote;
    rule.htmlNote = data.markdown_note !== undefined ? data.markdown_note : rule.htmlNote;
    rule.name = data.name !== undefined ? data.name : rule.name;
    if (template && data.params) {
      rule.params = [];
      data.params.split(';').forEach((param) => {
        const parts = param.split('=');
        const paramsDef = template.params?.find((p) => p.key === parts[0]);
        rule.params?.push({
          key: parts[0],
          type: paramsDef?.type || 'STRING',
          defaultValue: trim(parts[1], '" '),
          htmlDesc: paramsDef?.htmlDesc,
        });
      });
    }

    rule.remFnBaseEffort =
      data.remediation_fn_base_effort !== undefined
        ? data.remediation_fn_base_effort
        : rule.remFnBaseEffort;
    rule.remFnType =
      data.remediation_fn_type !== undefined ? data.remediation_fn_type : rule.remFnType;
    rule.severity = data.severity !== undefined ? data.severity : rule.severity;
    rule.status = data.status !== undefined ? data.status : rule.status;
    rule.tags = data.tags !== undefined ? data.tags.split(';') : rule.tags;

    return this.reply(rule);
  };

  handleSearchRules = ({ facets, languages, p, ps, rule_key }: SearchRulesQuery) => {
    const countFacet = (facets || '').split(',').map((facet: keyof Rule) => {
      const facetCount = countBy(
        this.rules.map((r) => r[FACET_RULE_MAP[facet] || facet] as string)
      );
      return {
        property: facet,
        values: Object.keys(facetCount).map((val) => ({ val, count: facetCount[val] })),
      };
    });
    const currentPs = ps || 10;
    const currentP = p || 1;
    let filteredRules: Rule[] = [];
    if (rule_key) {
      filteredRules = this.getRuleWithoutDetails().filter((r) => r.key === rule_key);
    } else {
      filteredRules = this.filterFacet({ languages });
    }
    const responseRules = filteredRules.slice((currentP - 1) * currentPs, currentP * currentPs);
    return this.reply({
      total: filteredRules.length,
      p: currentP,
      ps: currentPs,
      rules: responseRules,
      facets: countFacet,
    });
  };

  handleBulkActivateRules = () => {
    if (this.applyWithWarning) {
      return this.reply({
        succeeded: this.rules.length - 1,
        failed: 1,
        errors: [{ msg: 'c rule c:S6069 cannot be activated on cpp profile SonarSource' }],
      });
    }
    return this.reply({
      succeeded: this.rules.length,
      failed: 0,
      errors: [],
    });
  };

  handleBulkDeactivateRules = () => {
    return this.reply({
      succeeded: this.rules.length,
      failed: 0,
    });
  };

  handleSearchQualityProfiles = ({
    language,
  }: SearchQualityProfilesParameters = {}): Promise<SearchQualityProfilesResponse> => {
    let profiles: Profile[] = this.isAdmin
      ? this.qualityProfile.map((p) => ({ ...p, actions: { edit: true } }))
      : this.qualityProfile;
    if (language) {
      profiles = profiles.filter((p) => p.language === language);
    }
    return this.reply({ profiles });
  };

  handleGetRulesApp = () => {
    return this.reply({ canWrite: this.isAdmin, repositories: this.repositories });
  };

  handleGetCurrentUser = () => {
    return this.reply(
      mockCurrentUser({
        dismissedNotices: {
          educationPrinciples: this.dismissedNoticesEP,
        },
      })
    );
  };

  handleDismissNotification = (noticeType: NoticeType) => {
    if (noticeType === NoticeType.EDUCATION_PRINCIPLES) {
      this.dismissedNoticesEP = true;
      return this.reply(true);
    }

    return Promise.reject();
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
