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

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { isDefined } from '../../../helpers/types';
import {
  AzureRepository,
  BitbucketCloudRepository,
  BitbucketRepository,
  GithubRepository,
  GitlabProject,
} from '../../../types/alm-integration';
import { AlmInstanceBase, AlmKeys } from '../../../types/alm-settings';
import { DopSetting } from '../../../types/dop-translation';
import { Dict, Paging } from '../../../types/types';
import { REPOSITORY_PAGE_SIZE } from './constants';

type RepoTypes =
  | AzureRepository
  | BitbucketRepository
  | BitbucketCloudRepository
  | GithubRepository
  | GitlabProject;
type RepoCollectionTypes = Dict<RepoTypes[]> | RepoTypes[];

export function useProjectCreate<
  RepoType extends RepoTypes,
  RepoCollectionType extends RepoCollectionTypes,
  GroupType,
>(almKey: AlmKeys, dopSettings: DopSetting[], getKey: (repo: RepoType) => string) {
  const [isInitialized, setIsInitialized] = useState(false);
  const [selectedDopSetting, setSelectedDopSetting] = useState<DopSetting>();
  const [isLoadingOrganizations, setIsLoadingOrganizations] = useState(true);
  const [organizations, setOrganizations] = useState<GroupType[]>();
  const [selectedOrganization, setSelectedOrganization] = useState<GroupType>();
  const [isLoadingRepositories, setIsLoadingRepositories] = useState<boolean>(false);
  const [isLoadingMoreRepositories, setIsLoadingMoreRepositories] = useState<boolean>(false);
  const [repositories, setRepositories] = useState<RepoCollectionType>();
  const [selectedRepository, setSelectedRepository] = useState<RepoType>();
  const [showPersonalAccessTokenForm, setShowPersonalAccessTokenForm] = useState<boolean>(true);
  const [resetPersonalAccessToken, setResetPersonalAccessToken] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [projectsPaging, setProjectsPaging] = useState<Paging>({
    pageIndex: 1,
    pageSize: REPOSITORY_PAGE_SIZE,
    total: 0,
  });

  const router = useRouter();
  const location = useLocation();

  const isMonorepoSetup = location.query?.mono === 'true';
  const hasDopSettings = useMemo(() => Boolean(dopSettings?.length), [dopSettings]);

  const almInstances = useMemo(
    () =>
      dopSettings?.map((dopSetting) => ({
        alm: dopSetting.type,
        key: dopSetting.key,
        url: dopSetting.url,
      })) ?? [],
    [dopSettings],
  );

  const selectedAlmInstance = useMemo(
    () =>
      selectedDopSetting && {
        alm: selectedDopSetting.type,
        key: selectedDopSetting.key,
        url: selectedDopSetting.url,
      },
    [selectedDopSetting],
  );

  const cleanUrl = useCallback(() => {
    delete location.query.resetPat;
    router.replace(location);
  }, [location, router]);

  const handlePersonalAccessTokenCreated = useCallback(() => {
    cleanUrl();
    setShowPersonalAccessTokenForm(false);
    setResetPersonalAccessToken(false);
  }, [cleanUrl]);

  const onSelectDopSetting = useCallback((setting: DopSetting | undefined) => {
    setIsInitialized(false);
    setSelectedDopSetting(setting);
    setShowPersonalAccessTokenForm(true);
    setOrganizations([]);
    setRepositories(undefined);
    setSearchQuery('');
  }, []);

  const resetLoading = useCallback((value: boolean, more = false) => {
    if (more) {
      setIsLoadingMoreRepositories(value);
    } else {
      setIsLoadingRepositories(value);
    }
  }, []);

  const onSelectedAlmInstanceChange = useCallback(
    (instance?: AlmInstanceBase) => {
      onSelectDopSetting(
        instance ? dopSettings.find((dopSetting) => dopSetting.key === instance.key) : undefined,
      );
    },
    [dopSettings, onSelectDopSetting],
  );

  const handleSelectRepository = useCallback(
    (repositoryKey: string) => {
      if (Array.isArray(repositories)) {
        const repos = repositories as RepoType[];
        setSelectedRepository(repos.find((repo) => getKey(repo) === repositoryKey));
      } else {
        const repos = repositories as Dict<RepoType[]>;
        const selected = Object.values(repos)
          .flat()
          .find((repo) => getKey(repo) === repositoryKey);
        setSelectedRepository(selected);
      }
    },
    [getKey, repositories, setSelectedRepository],
  );

  useEffect(() => {
    if (!hasDopSettings || (hasDopSettings && isDefined(selectedDopSetting))) {
      return;
    }

    if (almKey === AlmKeys.GitHub) {
      const selectedDopSettingId = location.query?.dopSetting;
      if (selectedDopSettingId !== undefined) {
        const selectedDopSetting = dopSettings.find(({ id }) => id === selectedDopSettingId);

        if (selectedDopSetting) {
          setSelectedDopSetting(selectedDopSetting);
        }

        return;
      }
    }

    if (dopSettings.length > 1) {
      setSelectedDopSetting(undefined);
    } else {
      setSelectedDopSetting(dopSettings[0]);
    }
  }, [almKey, dopSettings, hasDopSettings, location, selectedDopSetting, setSelectedDopSetting]);

  return {
    almInstances,
    handlePersonalAccessTokenCreated,
    handleSelectRepository,
    hasDopSettings,
    isInitialized,
    isLoadingOrganizations,
    isLoadingRepositories,
    isLoadingMoreRepositories,
    isMonorepoSetup,
    onSelectedAlmInstanceChange,
    onSelectDopSetting,
    projectsPaging,
    organizations,
    repositories,
    resetLoading,
    resetPersonalAccessToken,
    searchQuery,
    selectedDopSetting,
    selectedRepository,
    setIsInitialized,
    setIsLoadingRepositories,
    setIsLoadingMoreRepositories,
    setIsLoadingOrganizations,
    setProjectsPaging,
    setOrganizations,
    selectedAlmInstance,
    selectedOrganization,
    setRepositories,
    setResetPersonalAccessToken,
    setSearchQuery,
    setSelectedDopSetting,
    setSelectedOrganization,
    setSelectedRepository,
    setShowPersonalAccessTokenForm,
    showPersonalAccessTokenForm,
  };
}
