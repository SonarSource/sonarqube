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
import { getStandards } from '../../helpers/security-standard';
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
import { SecurityStandard, Standards } from '../../types/security';
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
  getRuleRepositories,
  getRuleTags,
  getRulesApp,
  searchRules,
  updateRule,
} from '../rules';
import { dismissNotice, getCurrentUser } from '../users';

type FacetFilter = Pick<
  SearchRulesQuery,
  | 'languages'
  | 'tags'
  | 'available_since'
  | 'q'
  | 'types'
  | 'severities'
  | 'repositories'
  | 'qprofile'
  | 'sonarsourceSecurity'
  | 'owaspTop10'
  | 'owaspTop10-2021'
  | 'cwe'
  | 'is_template'
>;

const FACET_RULE_MAP: { [key: string]: keyof Rule } = {
  languages: 'lang',
  types: 'type',
  severities: 'severity',
  statuses: 'status',
  tags: 'tags',
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
  standardsToRules: Partial<{ [category in keyof Standards]: { [standard: string]: string[] } }> =
    {};

  qualityProfilesToRules: { [qp: string]: string[] } = {};

  constructor() {
    this.repositories = [
      mockRuleRepository({ key: 'repo1', name: 'Repository 1' }),
      mockRuleRepository({ key: 'repo2', name: 'Repository 2' }),
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
        repo: 'repo1',
        type: 'BUG',
        lang: 'java',
        langName: 'Java',
        name: 'Awsome java rule',
        tags: ['awesome'],
        params: [
          { key: '1', type: 'TEXT', htmlDesc: 'html description for key 1' },
          { key: '2', type: 'NUMBER', defaultValue: 'default value for key 2' },
        ],
      }),
      mockRuleDetails({
        key: 'rule2',
        repo: 'repo1',
        name: 'Hot hotspot',
        tags: ['awesome'],
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
      mockRuleDetails({
        key: 'rule3',
        repo: 'repo2',
        name: 'Unknown rule',
        lang: 'js',
        langName: 'JavaScript',
      }),
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
        tags: ['awesome', 'cute'],
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

    this.standardsToRules = {
      [SecurityStandard.SONARSOURCE]: {
        'buffer-overflow': ['rule1', 'rule2', 'rule3', 'rule4', 'rule5', 'rule6'],
      },
      [SecurityStandard.OWASP_TOP10_2021]: {
        a2: ['rule1', 'rule2', 'rule3', 'rule4', 'rule5'],
      },
      [SecurityStandard.OWASP_TOP10]: {
        a3: ['rule1', 'rule2', 'rule3', 'rule4'],
      },
      [SecurityStandard.CWE]: {
        '102': ['rule1', 'rule2', 'rule3'],
        '297': ['rule1', 'rule4'],
      },
    };

    this.qualityProfilesToRules = {
      p3: ['rule1', 'rule2', 'rule3', 'rule4', 'rule5', 'rule6', 'rule7', 'rule8'],
    };

    jest.mocked(updateRule).mockImplementation(this.handleUpdateRule);
    jest.mocked(createRule).mockImplementation(this.handleCreateRule);
    jest.mocked(deleteRule).mockImplementation(this.handleDeleteRule);
    jest.mocked(searchRules).mockImplementation(this.handleSearchRules);
    jest.mocked(getRuleDetails).mockImplementation(this.handleGetRuleDetails);
    jest.mocked(getRuleRepositories).mockImplementation(this.handleGetRuleRepositories);
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
    repositories,
    qprofile,
    sonarsourceSecurity,
    owaspTop10,
    'owaspTop10-2021': owasp2021Top10,
    cwe,
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
    if (repositories) {
      filteredRules = filteredRules.filter((r) => r.lang && repositories.includes(r.repo));
    }
    if (qprofile) {
      const rules = this.qualityProfilesToRules[qprofile] ?? [];
      filteredRules = filteredRules.filter((r) => rules.includes(r.key));
    }
    if (sonarsourceSecurity) {
      const matchingRules =
        this.standardsToRules[SecurityStandard.SONARSOURCE]?.[sonarsourceSecurity] ?? [];
      filteredRules = filteredRules.filter((r) => matchingRules.includes(r.key));
    }
    if (owasp2021Top10) {
      const matchingRules =
        this.standardsToRules[SecurityStandard.OWASP_TOP10_2021]?.[owasp2021Top10] ?? [];
      filteredRules = filteredRules.filter((r) => matchingRules.includes(r.key));
    }
    if (owaspTop10) {
      const matchingRules = this.standardsToRules[SecurityStandard.OWASP_TOP10]?.[owaspTop10] ?? [];
      filteredRules = filteredRules.filter((r) => matchingRules.includes(r.key));
    }
    if (cwe) {
      const matchingRules = this.standardsToRules[SecurityStandard.CWE]?.[cwe] ?? [];
      filteredRules = filteredRules.filter((r) => matchingRules.includes(r.key));
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

  handleGetRuleRepositories = (parameters: {
    q: string;
  }): Promise<Array<{ key: string; language: string; name: string }>> => {
    return this.reply(this.repositories.filter((r) => r.name.includes(parameters.q)));
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

  handleSearchRules = async ({
    facets,
    types,
    languages,
    p,
    ps,
    available_since,
    severities,
    repositories,
    qprofile,
    sonarsourceSecurity,
    owaspTop10,
    'owaspTop10-2021': owasp2021Top10,
    cwe,
    tags,
    q,
    rule_key,
    is_template,
  }: SearchRulesQuery): Promise<SearchRulesResponse> => {
    const standards = await getStandards();
    const facetCounts: Array<{ property: string; values: { val: string; count: number }[] }> = [];
    for (const facet of facets?.split(',') ?? []) {
      // If we can count facet values from the list of rules
      if (FACET_RULE_MAP[facet]) {
        const counts = countBy(this.rules.map((r) => r[FACET_RULE_MAP[facet]]));
        const values = Object.keys(counts).map((val) => ({ val, count: counts[val] }));
        facetCounts.push({
          property: facet,
          values,
        });
      } else if (facet === 'repositories') {
        facetCounts.push({
          property: facet,
          values: this.repositories.map((repo) => ({
            val: repo.key,
            count: this.rules.filter((r) => r.repo === repo.key).length,
          })),
        });
      } else if (typeof (standards as Dict<object>)[facet] === 'object') {
        // When a standards facet is requested, we return all the values with a count of 1
        facetCounts.push({
          property: facet,
          values: Object.keys((standards as any)[facet]).map((val: string) => ({
            val,
            count: 1,
          })),
        });
      } else {
        facetCounts.push({
          property: facet,
          values: [],
        });
      }
    }
    const currentPs = ps ?? 10;
    const currentP = p ?? 1;
    let filteredRules: Rule[] = [];
    if (rule_key) {
      filteredRules = this.getRulesWithoutDetails(this.rules).filter((r) => r.key === rule_key);
    } else {
      filteredRules = this.filterFacet({
        languages,
        available_since,
        q,
        severities,
        repositories,
        types,
        tags,
        is_template,
        qprofile,
        sonarsourceSecurity,
        owaspTop10,
        'owaspTop10-2021': owasp2021Top10,
        cwe,
      });
    }
    const responseRules = filteredRules.slice((currentP - 1) * currentPs, currentP * currentPs);
    return this.reply({
      rules: responseRules,
      facets: facetCounts,
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
