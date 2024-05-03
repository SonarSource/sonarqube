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
import { LabelValueSelectOption } from 'design-system';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { getGithubOrganizations, getGithubRepositories } from '../../../../api/alm-integrations';
import { GithubOrganization, GithubRepository } from '../../../../types/alm-integration';
import { AlmInstanceBase, AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { ImportProjectParam } from '../CreateProjectPage';
import { REPOSITORY_PAGE_SIZE } from '../constants';
import MonorepoProjectCreate from '../monorepo/MonorepoProjectCreate';
import { CreateProjectModes } from '../types';
import { useProjectCreate } from '../useProjectCreate';
import { useRepositorySearch } from '../useRepositorySearch';
import GitHubProjectCreateRenderer from './GitHubProjectCreateRenderer';
import { redirectToGithub } from './utils';

interface Props {
  isLoadingBindings: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
  dopSettings: DopSetting[];
}

export default function GitHubProjectCreate(props: Readonly<Props>) {
  const { dopSettings, isLoadingBindings, onProjectSetupDone } = props;

  const {
    almInstances,
    handleSelectRepository,
    isInitialized,
    isLoadingOrganizations,
    isLoadingRepositories,
    isMonorepoSetup,
    onSelectedAlmInstanceChange,
    onSelectDopSetting,
    projectsPaging,
    organizations,
    repositories,
    searchQuery,
    selectedAlmInstance,
    selectedDopSetting,
    selectedRepository,
    setIsInitialized,
    setIsLoadingRepositories,
    setProjectsPaging,
    setOrganizations,
    setRepositories,
    setSearchQuery,
    setSelectedOrganization,
    selectedOrganization,
    setIsLoadingOrganizations,
  } = useProjectCreate<GithubRepository, GithubRepository[], GithubOrganization>(
    AlmKeys.GitHub,
    dopSettings,
    ({ key }) => key,
  );

  const [isInError, setIsInError] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  const location = useLocation();
  const router = useRouter();

  const organizationOptions = useMemo(() => {
    return organizations?.map(transformToOption);
  }, [organizations]);
  const repositoryOptions = useMemo(() => {
    return repositories?.map(transformToOption);
  }, [repositories]);

  const fetchRepositories = useCallback(
    (orgKey: string, query?: string, pageIndex = 1) => {
      if (selectedDopSetting === undefined) {
        setIsInError(true);
        return Promise.resolve();
      }

      setIsLoadingRepositories(true);

      return getGithubRepositories({
        almSetting: selectedDopSetting.key,
        organization: orgKey,
        pageSize: REPOSITORY_PAGE_SIZE,
        page: pageIndex,
        query,
      })
        .then(({ paging, repositories }) => {
          setProjectsPaging(paging);
          setRepositories((prevRepositories) =>
            pageIndex === 1 ? repositories : [...(prevRepositories ?? []), ...repositories],
          );
          setIsInitialized(true);
        })
        .finally(() => {
          setIsLoadingRepositories(false);
        })
        .catch(() => {
          setProjectsPaging({ pageIndex: 1, pageSize: REPOSITORY_PAGE_SIZE, total: 0 });
          setRepositories([]);
        });
    },
    [
      selectedDopSetting,
      setIsInitialized,
      setIsLoadingRepositories,
      setProjectsPaging,
      setRepositories,
    ],
  );

  const onSelectDopSettingReauthenticate = useCallback(
    (setting?: DopSetting) => {
      onSelectDopSetting(setting);
      setIsAuthenticated(false);
    },
    [onSelectDopSetting],
  );

  const onSelectAlmSettingReauthenticate = useCallback(
    (setting?: AlmInstanceBase) => {
      onSelectedAlmInstanceChange(setting);
      setIsAuthenticated(false);
    },
    [onSelectedAlmInstanceChange],
  );

  const handleImportRepository = useCallback(
    (repoKeys: string[]) => {
      if (selectedDopSetting && selectedOrganization && repoKeys.length > 0) {
        onProjectSetupDone({
          almSetting: selectedDopSetting.key,
          creationMode: CreateProjectModes.GitHub,
          monorepo: false,
          projects: repoKeys.map((repositoryKey) => ({ repositoryKey })),
        });
      }
    },
    [onProjectSetupDone, selectedDopSetting, selectedOrganization],
  );

  const handleLoadMore = useCallback(() => {
    if (selectedOrganization) {
      fetchRepositories(selectedOrganization.key, searchQuery, projectsPaging.pageIndex + 1);
    }
  }, [fetchRepositories, projectsPaging.pageIndex, searchQuery, selectedOrganization]);

  const handleSelectOrganization = useCallback(
    (organizationKey: string) => {
      setSearchQuery('');
      setSelectedOrganization(organizations?.find(({ key }) => key === organizationKey));
    },
    [organizations, setSearchQuery, setSelectedOrganization],
  );

  useEffect(() => {
    if (selectedDopSetting?.url === undefined) {
      setIsInError(true);
      return;
    }
    setIsInError(false);

    const code = location.query?.code;
    if (!isAuthenticated) {
      if (code === undefined) {
        redirectToGithub({ isMonorepoSetup, selectedDopSetting }).catch(() => {
          setIsInError(true);
        });
      } else {
        setIsAuthenticated(true);
        delete location.query.code;
        router.replace(location);

        getGithubOrganizations(selectedDopSetting.key, code)
          .then(({ organizations }) => {
            setOrganizations(organizations);
            setIsLoadingOrganizations(false);
          })
          .catch(() => {
            setIsInError(true);
          });
      }
    }
    // Disabling rule as it causes an infinite loop and should only be called for dopSetting changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDopSetting]);

  const { onSearch } = useRepositorySearch(
    AlmKeys.GitHub,
    fetchRepositories,
    isInitialized,
    selectedDopSetting,
    selectedOrganization?.key,
    setSearchQuery,
  );

  return isMonorepoSetup ? (
    <MonorepoProjectCreate
      dopSettings={dopSettings}
      error={isInError}
      loadingBindings={isLoadingBindings}
      loadingOrganizations={isLoadingOrganizations}
      loadingRepositories={isLoadingRepositories}
      onProjectSetupDone={onProjectSetupDone}
      onSearchRepositories={onSearch}
      onSelectDopSetting={onSelectDopSettingReauthenticate}
      onSelectOrganization={handleSelectOrganization}
      onSelectRepository={handleSelectRepository}
      organizationOptions={organizationOptions}
      repositoryOptions={repositoryOptions}
      repositorySearchQuery={searchQuery}
      selectedDopSetting={selectedDopSetting}
      selectedOrganization={selectedOrganization && transformToOption(selectedOrganization)}
      selectedRepository={selectedRepository && transformToOption(selectedRepository)}
      showOrganizations
    />
  ) : (
    <GitHubProjectCreateRenderer
      almInstances={almInstances}
      error={isInError}
      loadingBindings={isLoadingBindings}
      loadingOrganizations={isLoadingOrganizations}
      loadingRepositories={isLoadingRepositories}
      onImportRepository={handleImportRepository}
      onLoadMore={handleLoadMore}
      onSearch={onSearch}
      onSelectedAlmInstanceChange={onSelectAlmSettingReauthenticate}
      onSelectOrganization={handleSelectOrganization}
      organizations={organizations}
      repositories={repositories}
      repositoryPaging={projectsPaging}
      searchQuery={searchQuery}
      selectedAlmInstance={selectedAlmInstance}
      selectedOrganization={selectedOrganization}
    />
  );
}

function transformToOption({
  key,
  name,
}: GithubOrganization | GithubRepository): LabelValueSelectOption<string> {
  return { value: key, label: name };
}
