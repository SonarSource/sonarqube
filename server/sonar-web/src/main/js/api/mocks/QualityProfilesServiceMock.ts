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
import { cloneDeep } from 'lodash';
import { ProfileChangelogEvent } from '../../apps/quality-profiles/types';
import { RequestData } from '../../helpers/request';
import {
  mockCompareResult,
  mockGroup,
  mockPaging,
  mockQualityProfile,
  mockQualityProfileChangelogEvent,
  mockRuleDetails,
  mockUserSelected,
} from '../../helpers/testMocks';
import {
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../types/clean-code-taxonomy';
import { SearchRulesResponse } from '../../types/coding-rules';
import { IssueSeverity } from '../../types/issues';
import { SearchRulesQuery } from '../../types/rules';
import { Dict, Paging, ProfileInheritanceDetails, RuleDetails } from '../../types/types';
import {
  CompareResponse,
  Profile,
  ProfileProject,
  SearchQualityProfilesParameters,
  SearchQualityProfilesResponse,
  activateRule,
  addGroup,
  addUser,
  associateProject,
  changeProfileParent,
  compareProfiles,
  copyProfile,
  createQualityProfile,
  deactivateRule,
  deleteProfile,
  dissociateProject,
  getExporters,
  getImporters,
  getProfileChangelog,
  getProfileInheritance,
  getProfileProjects,
  getQualityProfile,
  getQualityProfileExporterUrl,
  removeGroup,
  removeUser,
  renameProfile,
  restoreQualityProfile,
  searchGroups,
  searchQualityProfiles,
  searchUsers,
  setDefaultProfile,
} from '../quality-profiles';
import { getRuleDetails, searchRules, listRules } from '../rules';

jest.mock('../../api/rules');

export default class QualityProfilesServiceMock {
  isAdmin = false;
  listQualityProfile: Profile[] = [];
  languageMapping: Dict<Partial<Profile>> = {
    c: { language: 'c', languageName: 'C' },
    php: { language: 'php', languageName: 'PHP' },
    java: { language: 'java', languageName: 'Java' },
  };

  comparisonResult: CompareResponse = mockCompareResult();
  searchRulesResponse: SearchRulesResponse = {
    rules: [],
    paging: mockPaging(),
  };

  profileProjects: {
    [profileKey: string]: ProfileProject[];
  } = {};

  changelogEvents: ProfileChangelogEvent[] = [];

  constructor() {
    this.resetQualityProfile();
    this.resetComparisonResult();
    this.resetChangelogEvents();

    jest.mocked(searchQualityProfiles).mockImplementation(this.handleSearchQualityProfiles);
    jest.mocked(createQualityProfile).mockImplementation(this.handleCreateQualityProfile);
    jest.mocked(changeProfileParent).mockImplementation(this.handleChangeProfileParent);
    jest.mocked(getProfileInheritance).mockImplementation(this.handleGetProfileInheritance);
    jest.mocked(getProfileProjects).mockImplementation(this.handleGetProfileProjects);
    jest.mocked(copyProfile).mockImplementation(this.handleCopyProfile);
    jest.mocked(getImporters).mockImplementation(this.handleGetImporters);
    jest.mocked(searchRules).mockImplementation(this.handleSearchRules);
    jest.mocked(listRules).mockImplementation(this.handleListRules);
    jest.mocked(compareProfiles).mockImplementation(this.handleCompareQualityProfiles);
    jest.mocked(activateRule).mockImplementation(this.handleActivateRule);
    jest.mocked(deactivateRule).mockImplementation(this.handleDeactivateRule);
    jest.mocked(getRuleDetails).mockImplementation(this.handleGetRuleDetails);
    jest.mocked(restoreQualityProfile).mockImplementation(this.handleRestoreQualityProfile);
    jest.mocked(searchUsers).mockImplementation(this.handleSearchUsers);
    jest.mocked(addUser).mockImplementation(this.handleAddUser);
    jest.mocked(searchGroups).mockImplementation(this.handleSearchGroups);
    jest.mocked(addGroup).mockImplementation(this.handleAddGroup);
    jest.mocked(removeGroup).mockImplementation(this.handleRemoveGroup);
    jest.mocked(removeUser).mockImplementation(this.handleRemoveUser);
    jest.mocked(associateProject).mockImplementation(this.handleAssociateProject);
    jest.mocked(getProfileChangelog).mockImplementation(this.handleGetProfileChangelog);
    jest.mocked(dissociateProject).mockImplementation(this.handleDissociateProject);
    jest.mocked(getQualityProfile).mockImplementation(this.handleGetQualityProfile);
    jest.mocked(getExporters).mockImplementation(this.handleGetExporters);
    jest.mocked(deleteProfile).mockImplementation(this.handleDeleteProfile);
    jest.mocked(renameProfile).mockImplementation(this.handleRenameProfile);
    jest.mocked(setDefaultProfile).mockImplementation(this.handleSetDefaultProfile);
    jest
      .mocked(getQualityProfileExporterUrl)
      .mockImplementation(() => '/api/qualityprofiles/export');
  }

  resetQualityProfile() {
    this.listQualityProfile = [
      mockQualityProfile({
        key: 'c-qp',
        language: 'c',
        languageName: 'C',
        name: 'c quality profile',
        activeDeprecatedRuleCount: 0,
      }),
      mockQualityProfile({
        key: 'java-qp',
        language: 'java',
        languageName: 'Java',
        name: 'java quality profile',
        activeDeprecatedRuleCount: 0,
      }),
      mockQualityProfile({
        key: 'java-qp-1',
        language: 'java',
        languageName: 'Java',
        name: 'java quality profile #2',
        activeDeprecatedRuleCount: 1,
        actions: {
          edit: this.isAdmin,
        },
      }),
      mockQualityProfile({
        key: 'sonar',
        language: 'java',
        languageName: 'Java',
        name: 'Sonar way',
        isBuiltIn: true,
        isDefault: true,
      }),
      mockQualityProfile({
        key: 'old-php-qp',
        language: 'php',
        languageName: 'PHP',
        name: 'Good old PHP quality profile',
        activeDeprecatedRuleCount: 8,
        rulesUpdatedAt: '2019-09-16T21:10:36+0000',
        parentKey: 'php-sonar-way-1',
        actions: {
          edit: this.isAdmin,
          associateProjects: this.isAdmin,
          delete: this.isAdmin,
          setAsDefault: this.isAdmin,
          copy: this.isAdmin,
        },
      }),
      mockQualityProfile({
        key: 'no-rule-qp',
        activeRuleCount: 0,
        actions: {
          associateProjects: true,
          setAsDefault: this.isAdmin,
        },
      }),
      mockQualityProfile({
        activeRuleCount: 6,
        isBuiltIn: true,
        key: 'php-sonar-way-1',
        name: 'PHP Sonar way 1',
        language: 'php',
        languageName: 'PHP',
      }),
      mockQualityProfile({
        activeRuleCount: 6,
        isBuiltIn: false,
        key: 'php-sonar-way-2',
        name: 'PHP Sonar way 2',
        language: 'php',
        languageName: 'PHP',
      }),
      mockQualityProfile({
        activeRuleCount: 3,
        isBuiltIn: false,
        key: 'php-sonar-way',
        parentKey: 'old-php-qp',
        name: 'PHP way',
        language: 'php',
        languageName: 'PHP',
      }),
    ];
  }

  resetComparisonResult() {
    this.comparisonResult = mockCompareResult();
  }

  resetChangelogEvents() {
    this.changelogEvents = [
      mockQualityProfileChangelogEvent({
        date: '2019-05-23T04:12:32+0100',
      }),
      mockQualityProfileChangelogEvent({
        date: '2019-05-23T03:12:32+0100',
        action: 'DEACTIVATED',
        ruleKey: 'php:rule1',
        ruleName: 'PHP Rule',
        params: {
          severity: IssueSeverity.Critical,
          newCleanCodeAttribute: CleanCodeAttribute.Complete,
          newCleanCodeAttributeCategory: CleanCodeAttributeCategory.Intentional,
          oldCleanCodeAttribute: CleanCodeAttribute.Clear,
          oldCleanCodeAttributeCategory: CleanCodeAttributeCategory.Responsible,
        },
      }),
      mockQualityProfileChangelogEvent({
        date: '2019-05-23T03:12:32+0100',
        action: 'ACTIVATED',
        ruleKey: 'c:rule0',
        ruleName: 'Rule 0',
        params: {},
      }),
      mockQualityProfileChangelogEvent({
        date: '2019-04-23T03:12:32+0100',
        action: 'DEACTIVATED',
        ruleKey: 'c:rule0',
        ruleName: 'Rule 0',
      }),
      mockQualityProfileChangelogEvent({
        date: '2019-04-23T03:12:32+0100',
        action: 'DEACTIVATED',
        ruleKey: 'c:rule1',
        ruleName: 'Rule 1',
        params: {
          severity: IssueSeverity.Critical,
          newCleanCodeAttribute: CleanCodeAttribute.Complete,
          newCleanCodeAttributeCategory: CleanCodeAttributeCategory.Intentional,
          oldCleanCodeAttribute: CleanCodeAttribute.Lawful,
          oldCleanCodeAttributeCategory: CleanCodeAttributeCategory.Responsible,
          impactChanges: [
            {
              newSeverity: SoftwareImpactSeverity.Medium,
              newSoftwareQuality: SoftwareQuality.Reliability,
            },
            {
              oldSeverity: SoftwareImpactSeverity.High,
              oldSoftwareQuality: SoftwareQuality.Maintainability,
            },
          ],
        },
      }),
      mockQualityProfileChangelogEvent({
        date: '2019-04-23T02:12:32+0100',
        action: 'DEACTIVATED',
        ruleKey: 'c:rule2',
        ruleName: 'Rule 2',
        authorName: 'John Doe',
      }),
      mockQualityProfileChangelogEvent({
        date: '2019-03-23T02:12:32+0100',
        ruleKey: 'c:rule2',
        ruleName: 'Rule 2',
        authorName: 'John Doe',
        params: {
          severity: IssueSeverity.Critical,
          credentialWords: 'foo,bar',
        },
      }),
    ];
  }

  resetSearchRulesResponse() {
    this.searchRulesResponse = {
      facets: [
        {
          property: 'types',
          values: [
            { val: 'CODE_SMELL', count: 250 },
            { val: 'BUG', count: 60 },
            { val: 'VULNERABILITY', count: 40 },
            { val: 'SECURITY_HOTSPOT', count: 50 },
          ],
        },
      ],
      rules: [],
      paging: {
        pageIndex: 1,
        pageSize: 400,
        total: 400,
      },
    };
  }

  resetProfileProjects() {
    this.profileProjects = {
      'old-php-qp': [
        {
          key: 'Benflix',
          name: 'Benflix',
          selected: true,
        },
        {
          key: 'Twitter',
          name: 'Twitter',
          selected: false,
        },
      ],
    };
  }

  handleGetImporters = () => {
    return this.reply([
      {
        key: 'sonar-importer-a',
        name: 'Importer A',
        languages: ['c'],
      },
      {
        key: 'sonar-importer-b',
        name: 'Importer B',
        languages: ['c'],
      },
    ]);
  };

  handleCopyProfile = (fromKey: string, name: string): Promise<Profile> => {
    const profile = this.listQualityProfile.find((p) => p.key === fromKey);
    if (!profile) {
      return Promise.reject({
        errors: [{ msg: `No profile has been found for  ${fromKey}` }],
      });
    }

    const copiedQualityProfile = mockQualityProfile({
      ...profile,
      name,
      key: `qp${this.listQualityProfile.length}`,
    });

    this.listQualityProfile.push(copiedQualityProfile);

    return this.reply(copiedQualityProfile);
  };

  handleGetProfileProjects = (
    data: RequestData,
  ): Promise<{
    more: boolean;
    paging: Paging;
    results: ProfileProject[];
  }> => {
    const results = (this.profileProjects[data.key] ?? []).filter(
      (project) =>
        project.selected ===
        (data.selected !== undefined ? Boolean(data.selected === 'selected') : true),
    );

    return this.reply({
      more: false,
      paging: {
        pageIndex: 0,
        pageSize: 10,
        total: 0,
      },
      results,
    });
  };

  handleGetProfileInheritance = ({
    language,
    name,
  }: Profile): Promise<{
    ancestors: ProfileInheritanceDetails[];
    children: ProfileInheritanceDetails[];
    profile: ProfileInheritanceDetails;
  }> => {
    const profileToProfileInheritanceDetails = (profile: Profile): ProfileInheritanceDetails => ({
      ...profile,
      inactiveRuleCount: 3,
      isBuiltIn: false,
    });

    const profile = this.listQualityProfile.find((p) => p.name === name && p.language === language);
    if (!profile) {
      return Promise.reject({
        errors: [{ msg: `No profile has been found for  ${language} ${name}` }],
      });
    }

    const ancestors = this.listQualityProfile
      .filter((p) => p.key === profile.parentKey)
      .map(profileToProfileInheritanceDetails);
    const children = this.listQualityProfile
      .filter((p) => p.parentKey === profile.key)
      .map(profileToProfileInheritanceDetails);

    return this.reply({
      ancestors,
      children,
      profile: profileToProfileInheritanceDetails(profile),
    });
  };

  handleChangeProfileParent = ({ language, name }: Profile, parentProfile?: Profile) => {
    const profile = this.listQualityProfile.find((p) => p.name === name && p.language === language);

    if (!profile) {
      return Promise.reject({
        errors: [{ msg: `No profile has been found for  ${language} ${name}` }],
      });
    }

    profile.parentKey = parentProfile?.key;
    profile.parentName = parentProfile?.name;

    return Promise.resolve({});
  };

  handleCreateQualityProfile = (data: RequestData | FormData) => {
    if (data instanceof FormData) {
      const name = data.get('name') as string;
      const language = data.get('language') as string;
      const newQualityProfile = mockQualityProfile({
        name,
        ...this.languageMapping[language],
        key: `qp${this.listQualityProfile.length}`,
      });

      this.listQualityProfile.push(newQualityProfile);

      return this.reply({ profile: newQualityProfile });
    }
    const newQualityProfile = mockQualityProfile({
      name: data.name,
      ...this.languageMapping[data.language],
      key: `qp${this.listQualityProfile.length}`,
    });

    this.listQualityProfile.push(newQualityProfile);

    return this.reply({ profile: newQualityProfile });
  };

  handleSearchRules = (data: SearchRulesQuery): Promise<SearchRulesResponse> => {
    // Special case when we want rule breakdown
    if (data.facets === 'cleanCodeAttributeCategories,impactSoftwareQualities') {
      const activation = data.activation === 'true';
      return this.reply({
        facets: [
          {
            property: 'cleanCodeAttributeCategories',
            values: [
              {
                val: CleanCodeAttributeCategory.Intentional,
                count: activation ? 23 : 27,
              },
              {
                val: CleanCodeAttributeCategory.Consistent,
                count: activation ? 2 : 20,
              },
              {
                val: CleanCodeAttributeCategory.Adaptable,
                count: activation ? 1 : 12,
              },
              {
                val: CleanCodeAttributeCategory.Responsible,
                count: 0,
              },
            ],
          },
          {
            property: 'impactSoftwareQualities',
            values: [
              {
                val: SoftwareQuality.Maintainability,
                count: activation ? 9 : 53,
              },
              {
                val: SoftwareQuality.Reliability,
                count: activation ? 16 : 17,
              },
              {
                val: SoftwareQuality.Security,
                count: activation ? 0 : 14,
              },
            ],
          },
        ],
        rules: [],
        paging: mockPaging(),
      });
    }
    return this.reply(this.searchRulesResponse);
  };

  handleListRules = (data: SearchRulesQuery): Promise<SearchRulesResponse> => {
    // Both APIs are mocked the same way, this method is only here to make it explicit.
    return this.handleSearchRules(data);
  };

  handleGetQualityProfile = () => {
    return this.reply({
      profile: mockQualityProfile(),
      compareToSonarWay: {
        profile: '',
        profileName: 'Sonar way',
        missingRuleCount: 29,
      },
    });
  };

  handleSearchQualityProfiles = (
    parameters: SearchQualityProfilesParameters = {},
  ): Promise<SearchQualityProfilesResponse> => {
    const { language } = parameters;
    let profiles = this.listQualityProfile;
    if (language) {
      profiles = profiles.filter((p) => p.language === language);
    }
    if (this.isAdmin) {
      profiles = profiles.map((p) => ({ ...p, actions: { ...p.actions, copy: true } }));
    }

    return this.reply({
      actions: { create: this.isAdmin },
      profiles,
    });
  };

  handleActivateRule = (data: {
    key: string;
    params?: Dict<string>;
    reset?: boolean;
    rule: string;
    severity?: string;
  }): Promise<undefined> => {
    this.comparisonResult.inRight = this.comparisonResult.inRight.filter(
      ({ key }) => key !== data.rule,
    );

    return this.reply(undefined);
  };

  handleDeactivateRule = (data: { key: string; rule: string }) => {
    this.comparisonResult.inLeft = this.comparisonResult.inLeft.filter(
      ({ key }) => key !== data.rule,
    );

    return this.reply(undefined);
  };

  handleCompareQualityProfiles = (leftKey: string, rightKey: string): Promise<CompareResponse> => {
    const comparedProfiles = this.listQualityProfile.reduce((profiles, profile) => {
      if (profile.key === leftKey || profile.key === rightKey) {
        profiles.push(profile);
      }
      return profiles;
    }, [] as Profile[]);
    const [leftName, rightName] = comparedProfiles.map((profile) => profile.name);

    this.comparisonResult.left = { name: leftName };
    this.comparisonResult.right = { name: rightName };

    return this.reply(this.comparisonResult);
  };

  handleGetRuleDetails = (params: { key: string }): Promise<{ rule: RuleDetails }> => {
    return this.reply({
      rule: mockRuleDetails({
        key: params.key,
      }),
    });
  };

  handleRestoreQualityProfile = () => {
    return this.reply({
      profile: {
        key: 'c-sonarsource-06756',
        name: 'SonarSource',
        language: 'c',
        isDefault: false,
        isInherited: false,
        languageName: 'C',
      },
      ruleSuccesses: 231,
      ruleFailures: 0,
    });
  };

  handleSearchUsers = () => {
    return this.reply({
      users: [
        mockUserSelected({
          login: 'buzz',
          name: 'Buzz',
        }),
      ],
      paging: mockPaging(),
    });
  };

  handleAddUser = () => {
    return this.reply(undefined);
  };

  handleRemoveUser = () => {
    return this.reply(undefined);
  };

  handleSearchGroups = () => {
    return this.reply({
      groups: [mockGroup({ name: 'ACDC' })],
      paging: mockPaging(),
    });
  };

  handleAddGroup = () => {
    return this.reply(undefined);
  };

  handleRemoveGroup = () => {
    return this.reply(undefined);
  };

  handleAssociateProject = ({ key }: Profile, project: string) => {
    const projects = this.profileProjects[key].map((profileProject) => {
      if (profileProject.key === project) {
        return {
          ...profileProject,
          selected: true,
        };
      }
      return profileProject;
    });
    this.profileProjects[key] = projects;

    return this.reply({});
  };

  handleDissociateProject = ({ key }: Profile, project: string) => {
    const projects = this.profileProjects[key].map((profileProject) => {
      if (profileProject.key === project) {
        return {
          ...profileProject,
          selected: false,
        };
      }
      return profileProject;
    });
    this.profileProjects[key] = projects;

    return this.reply({});
  };

  handleGetProfileChangelog: typeof getProfileChangelog = (since, to, { language }, page) => {
    const PAGE_SIZE = 50;
    const p = page || 1;
    const events = this.changelogEvents.filter((event) => {
      if (event.ruleKey.split(':')[0] !== language) {
        return false;
      }
      if (since && new Date(since) >= new Date(event.date)) {
        return false;
      }
      if (to && new Date(to) <= new Date(event.date)) {
        return false;
      }
      return true;
    });

    return this.reply({
      events: events.slice((p - 1) * PAGE_SIZE, (p - 1) * PAGE_SIZE + PAGE_SIZE),
      paging: mockPaging({
        total: events.length,
        pageSize: PAGE_SIZE,
        pageIndex: p,
      }),
    });
  };

  handleGetExporters = () => {
    return this.reply([
      {
        key: 'sonarlint-vs',
        name: 'SonarLint for Visual Studio',
        languages: ['php'],
      },
      {
        key: 'sonarlint-eclipse',
        name: 'SonarLint for Eclipse',
        languages: ['php'],
      },
    ]);
  };

  handleDeleteProfile = ({ name }: Profile) => {
    // delete Children
    const qualityProfileToDelete = this.listQualityProfile.find((profile) => profile.name === name);
    this.listQualityProfile = this.listQualityProfile.filter(
      (profile) => profile.parentKey !== qualityProfileToDelete?.key,
    );

    // delete profile
    this.listQualityProfile = this.listQualityProfile.filter((profile) => profile.name !== name);

    return this.reply({});
  };

  handleRenameProfile = (key: string, newName: string) => {
    this.listQualityProfile = this.listQualityProfile.map((profile) => {
      if (profile.key === key) {
        return {
          ...profile,
          name: newName,
        };
      }
      return profile;
    });
    return this.reply({});
  };

  handleSetDefaultProfile = ({ name }: Profile) => {
    this.listQualityProfile = this.listQualityProfile.map((profile) => {
      if (profile.name === name) {
        return {
          ...profile,
          isDefault: true,
        };
      }
      return profile;
    });
    return Promise.resolve();
  };

  setAdmin() {
    this.isAdmin = true;
    this.resetQualityProfile();
  }

  setRulesSearchResponse(overrides: Partial<SearchRulesResponse>) {
    this.searchRulesResponse = {
      ...this.searchRulesResponse,
      ...overrides,
    };
  }

  reset() {
    this.isAdmin = false;
    this.resetQualityProfile();
    this.resetComparisonResult();
    this.resetSearchRulesResponse();
    this.resetProfileProjects();
    this.resetChangelogEvents();
  }

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
