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
import { cloneDeep } from 'lodash';
import { RequestData } from '../../helpers/request';
import { mockQualityProfile } from '../../helpers/testMocks';
import { SearchRulesResponse } from '../../types/coding-rules';
import { Dict, Paging, ProfileInheritanceDetails } from '../../types/types';
import {
  changeProfileParent,
  copyProfile,
  createQualityProfile,
  getImporters,
  getProfileInheritance,
  getProfileProjects,
  Profile,
  ProfileProject,
  searchQualityProfiles,
  SearchQualityProfilesParameters,
  SearchQualityProfilesResponse,
} from '../quality-profiles';
import { searchRules } from '../rules';

export default class QualityProfilesServiceMock {
  isAdmin = false;
  listQualityProfile: Profile[] = [];

  languageMapping: Dict<Partial<Profile>> = {
    c: { language: 'c', languageName: 'C' },
  };

  constructor() {
    this.resetQualityProfile();

    (searchQualityProfiles as jest.Mock).mockImplementation(this.handleSearchQualityProfiles);
    (createQualityProfile as jest.Mock).mockImplementation(this.handleCreateQualityProfile);
    (changeProfileParent as jest.Mock).mockImplementation(this.handleChangeProfileParent);
    (getProfileInheritance as jest.Mock).mockImplementation(this.handleGetProfileInheritance);
    (getProfileProjects as jest.Mock).mockImplementation(this.handleGetProfileProjects);
    (copyProfile as jest.Mock).mockImplementation(this.handleCopyProfile);
    (getImporters as jest.Mock).mockImplementation(this.handleGetImporters);
    (searchRules as jest.Mock).mockImplementation(this.handleSearchRules);
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
        name: 'java quality profile',
        activeDeprecatedRuleCount: 0,
      }),
    ];
  }

  handleGetImporters = () => {
    return this.reply([]);
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

  handleGetProfileProjects = (): Promise<{
    more: boolean;
    paging: Paging;
    results: ProfileProject[];
  }> => {
    return this.reply({
      more: false,
      paging: {
        pageIndex: 0,
        pageSize: 10,
        total: 0,
      },
      results: [],
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
    const profile = this.listQualityProfile.find((p) => p.name === name && p.language === language);
    if (!profile) {
      return Promise.reject({
        errors: [{ msg: `No profile has been found for  ${language} ${name}` }],
      });
    }

    // Lets fake it for now
    return this.reply({
      ancestors: [],
      children: [],
      profile: {
        name: profile.name,
        activeRuleCount: 0,
        isBuiltIn: false,
        key: profile.key,
      },
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

  handleSearchRules = (): Promise<SearchRulesResponse> => {
    return this.reply({
      p: 0,
      ps: 500,
      total: 0,
      rules: [],
    });
  };

  handleSearchQualityProfiles = (
    parameters: SearchQualityProfilesParameters = {}
  ): Promise<SearchQualityProfilesResponse> => {
    const { language } = parameters;
    let profiles = this.listQualityProfile;
    if (language) {
      profiles = profiles.filter((p) => p.language === language);
    }
    if (this.isAdmin) {
      profiles = profiles.map((p) => ({ ...p, actions: { copy: true } }));
    }

    return this.reply({
      actions: { create: this.isAdmin },
      profiles,
    });
  };

  setAdmin() {
    this.isAdmin = true;
  }

  reset() {
    this.isAdmin = false;
    this.resetQualityProfile();
  }

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
