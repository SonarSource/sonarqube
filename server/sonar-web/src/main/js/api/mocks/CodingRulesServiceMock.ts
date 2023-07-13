/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  mockPaging,
  mockQualityProfile,
  mockRuleActivation,
  mockRuleDetails,
  mockRuleRepository,
} from '../../helpers/testMocks';
import { RuleRepository, SearchRulesResponse } from '../../types/coding-rules';
import { RawIssuesResponse } from '../../types/issues';
import { SearchRulesQuery } from '../../types/rules';
import { Dict, Rule, RuleActivation, RuleDetails, RulesUpdateRequest } from '../../types/types';
import { NoticeType } from '../../types/users';
import { getFacet } from '../issues';
import {
  Profile,
  SearchQualityProfilesParameters,
  SearchQualityProfilesResponse,
  activateRule,
  bulkActivateRules,
  bulkDeactivateRules,
  deactivateRule,
  searchQualityProfiles,
} from '../quality-profiles';
import {
  CreateRuleData,
  createRule,
  deleteRule,
  getRuleDetails,
  getRuleTags,
  getRulesApp,
  searchRules,
  updateRule,
} from '../rules';
import { dismissNotice, getCurrentUser } from '../users';

interface FacetFilter {
  languages?: string;
  tags?: string;
  available_since?: string;
  q?: string;
  types?: string;
  severities?: string;
  is_template?: string | boolean;
}

const FACET_RULE_MAP: { [key: string]: keyof Rule } = {
  languages: 'lang',
  types: 'type',
};

export const RULE_TAGS_MOCK = ['awesome', 'cute', 'nice'];

export default class CodingRulesServiceMock {
  defaultRules: RuleDetails[] = [];
  defaultRulesActivations: Dict<RuleActivation[]> = {};
  rulesActivations: Dict<RuleActivation[]> = {};
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
      mockQualityProfile({
        key: 'p4',
        name: 'QP FooBarBaz',
        language: 'java',
        languageName: 'Java',
      }),
    ];

    const resourceContent = 'Some link <a href="http://example.com">Awsome Reading</a>';
    const introTitle = 'Introduction to this rule';
    const rootCauseContent = 'Root cause';
    const howToFixContent = 'This is how to fix';

    this.defaultRules = [
      mockRuleDetails({
        key: 'rule1',
        type: 'BUG',
        lang: 'java',
        langName: 'Java',
        name: 'Awsome java rule',
        params: [
          { key: '1', type: 'TEXT', htmlDesc: 'html description for key 1' },
          { key: '2', type: 'NUMBER', defaultValue: 'default value for key 2' },
        ],
      }),
      mockRuleDetails({
        key: 'rule2',
        name: 'Hot hotspot',
        type: 'SECURITY_HOTSPOT',
        lang: 'js',
        descriptionSections: [
          { key: RuleDescriptionSections.INTRODUCTION, content: introTitle },
          { key: RuleDescriptionSections.ROOT_CAUSE, content: rootCauseContent },
          { key: RuleDescriptionSections.HOW_TO_FIX, content: howToFixContent },
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
        type: 'BUG',
        lang: 'py',
        langName: 'Python',
        name: 'Bad Python rule',
        isExternal: true,
        descriptionSections: undefined,
      }),
      mockRuleDetails({
        key: 'rule7',
        type: 'VULNERABILITY',
        severity: 'MINOR',
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
        type: 'BUG',
        severity: 'MINOR',
        lang: 'py',
        langName: 'Python',
        tags: ['awesome'],
        name: 'Template rule',
        params: [
          { key: '1', type: 'TEXT', htmlDesc: 'html description for key 1' },
          { key: '2', type: 'NUMBER', defaultValue: 'default value for key 2' },
        ],
        isTemplate: true,
      }),
      mockRuleDetails({
        key: 'rule9',
        type: 'BUG',
        severity: 'MINOR',
        lang: 'py',
        langName: 'Python',
        tags: ['awesome'],
        name: 'Custom Rule based on rule8',
        params: [
          { key: '1', type: 'TEXT', htmlDesc: 'html description for key 1' },
          { key: '2', type: 'NUMBER', defaultValue: 'default value for key 2' },
        ],
        templateKey: 'rule8',
      }),
      // Keep this last
      mockRuleDetails({
        createdAt: '2022-12-16T17:26:54+0100',
        key: 'rule10',
        type: 'VULNERABILITY',
        severity: 'MINOR',
        lang: 'py',
        langName: 'Python',
        tags: ['awesome'],
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

    this.defaultRulesActivations = {
      [this.defaultRules[0].key]: [mockRuleActivation({ qProfile: 'p1' })],
    };

    jest.mocked(updateRule).mockImplementation(this.handleUpdateRule);
    jest.mocked(createRule).mockImplementation(this.handleCreateRule);
    jest.mocked(deleteRule).mockImplementation(this.handleDeleteRule);
    jest.mocked(searchRules).mockImplementation(this.handleSearchRules);
    jest.mocked(getRuleDetails).mockImplementation(this.handleGetRuleDetails);
    jest.mocked(searchQualityProfiles).mockImplementation(this.handleSearchQualityProfiles);
    jest.mocked(getRulesApp).mockImplementation(this.handleGetRulesApp);
    jest.mocked(bulkActivateRules).mockImplementation(this.handleBulkActivateRules);
    jest.mocked(bulkDeactivateRules).mockImplementation(this.handleBulkDeactivateRules);
    jest.mocked(activateRule).mockImplementation(this.handleActivateRule);
    jest.mocked(deactivateRule).mockImplementation(this.handleDeactivateRule);
    jest.mocked(getFacet).mockImplementation(this.handleGetGacet);
    jest.mocked(getRuleTags).mockImplementation(this.handleGetRuleTags);
    jest.mocked(getCurrentUser).mockImplementation(this.handleGetCurrentUser);
    jest.mocked(dismissNotice).mockImplementation(this.handleDismissNotification);
    this.rules = cloneDeep(this.defaultRules);
    this.rulesActivations = cloneDeep(this.defaultRulesActivations);
  }

  getRulesWithoutDetails(rules: RuleDetails[]) {
    return rules.map((r) =>
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

  filterFacet({
    languages,
    available_since,
    q,
    severities,
    types,
    tags,
    is_template,
  }: FacetFilter) {
    let filteredRules = this.rules;
    if (types) {
      filteredRules = filteredRules.filter((r) => types.includes(r.type));
    }
    if (languages) {
      filteredRules = filteredRules.filter((r) => r.lang && languages.includes(r.lang));
    }
    if (severities) {
      filteredRules = filteredRules.filter((r) => r.severity && severities.includes(r.severity));
    }
    if (available_since) {
      filteredRules = filteredRules.filter(
        (r) => r.createdAt && new Date(r.createdAt) > new Date(available_since)
      );
    }
    if (is_template !== undefined) {
      filteredRules = filteredRules.filter((r) => (is_template ? r.isTemplate : !r.isTemplate));
    }
    if (q && q.length > 2) {
      filteredRules = filteredRules.filter((r) => r.name.includes(q));
    }

    if (tags) {
      filteredRules = filteredRules.filter((r) => r.tags && r.tags.some((t) => tags.includes(t)));
    }
    return this.getRulesWithoutDetails(filteredRules);
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
    this.rulesActivations = cloneDeep(this.defaultRulesActivations);
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
    return this.reply({
      actives: parameters.actives ? this.rulesActivations[rule.key] ?? [] : undefined,
      rule,
    });
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

  handleCreateRule = (data: CreateRuleData) => {
    const newRule = mockRuleDetails({
      descriptionSections: [
        { key: RuleDescriptionSections.DEFAULT, content: data.markdownDescription },
      ],
      ...pick(data, ['templateKey', 'severity', 'type', 'name', 'status']),
      key: data.customKey,
      params:
        data.params?.split(';').map((param: string) => {
          const [key, value] = param.split('=');
          return { key, defaultValue: value, type: 'TEXT' };
        }) ?? [],
    });

    this.rules.push(newRule);

    return this.reply(newRule);
  };

  handleDeleteRule = (data: { key: string }) => {
    this.rules = this.rules.filter((r) => r.key !== data.key);
    return this.reply(undefined);
  };

  handleSearchRules = ({
    facets,
    types,
    languages,
    p,
    ps,
    available_since,
    severities,
    tags,
    q,
    rule_key,
    is_template,
  }: SearchRulesQuery): Promise<SearchRulesResponse> => {
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
      filteredRules = this.getRulesWithoutDetails(this.rules).filter((r) => r.key === rule_key);
    } else {
      filteredRules = this.filterFacet({
        languages,
        available_since,
        q,
        severities,
        types,
        tags,
        is_template,
      });
    }
    const responseRules = filteredRules.slice((currentP - 1) * currentPs, currentP * currentPs);
    return this.reply({
      rules: responseRules,
      facets: countFacet,
      paging: mockPaging({
        total: filteredRules.length,
        pageIndex: currentP,
        pageSize: currentPs,
      }),
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

  handleActivateRule = (data: {
    key: string;
    params?: Dict<string>;
    reset?: boolean;
    rule: string;
    severity?: string;
  }) => {
    const nextActivation = mockRuleActivation({ qProfile: data.key, severity: data.severity });
    const activationIndex = this.rulesActivations[data.rule]?.findIndex((activation) => {
      return activation.qProfile === data.key;
    });
    if (activationIndex !== -1) {
      this.rulesActivations[data.rule][activationIndex] = nextActivation;
    } else {
      this.rulesActivations[data.rule] = [...this.rulesActivations[data.rule], nextActivation];
    }
    return this.reply(undefined);
  };

  handleDeactivateRule = (data: { key: string; rule: string }) => {
    this.rulesActivations[data.rule] = this.rulesActivations[data.rule]?.filter(
      (activation) => activation.qProfile !== data.key
    );
    return this.reply(undefined);
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

  handleGetRuleTags = (data: { ps?: number; q: string }) => {
    return this.reply(RULE_TAGS_MOCK.filter((tag) => tag.includes(data.q)));
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
