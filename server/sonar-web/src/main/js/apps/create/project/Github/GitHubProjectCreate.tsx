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
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getGithubOrganizations, getGithubRepositories } from '../../../../api/alm-integrations';
import { useLocation, useRouter } from '../../../../components/hoc/withRouter';
import { LabelValueSelectOption } from '../../../../helpers/search';
import { GithubOrganization, GithubRepository } from '../../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { Paging } from '../../../../types/types';
import { ImportProjectParam } from '../CreateProjectPage';
import MonorepoProjectCreate from '../monorepo/MonorepoProjectCreate';
import { CreateProjectModes } from '../types';
import GitHubProjectCreateRenderer from './GitHubProjectCreateRenderer';
import { redirectToGithub } from './utils';

interface Props {
  canAdmin: boolean;
  isLoadingBindings: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
  dopSettings: DopSetting[];
}

const REPOSITORY_PAGE_SIZE = 50;
const REPOSITORY_SEARCH_DEBOUNCE_TIME = 250;

export default function GitHubProjectCreate(props: Readonly<Props>) {
  const { canAdmin, dopSettings, isLoadingBindings, onProjectSetupDone } = props;

  const repositorySearchDebounceId = useRef<NodeJS.Timeout | undefined>();

  const [isInError, setIsInError] = useState(false);
  const [isLoadingOrganizations, setIsLoadingOrganizations] = useState(true);
  const [isLoadingRepositories, setIsLoadingRepositories] = useState(false);
  const [organizations, setOrganizations] = useState<GithubOrganization[]>([]);
  const [repositories, setRepositories] = useState<GithubRepository[]>([]);
  const [repositoryPaging, setRepositoryPaging] = useState<Paging>({
    pageSize: REPOSITORY_PAGE_SIZE,
    total: 0,
    pageIndex: 1,
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDopSetting, setSelectedDopSetting] = useState<DopSetting>();
  const [selectedOrganization, setSelectedOrganization] = useState<GithubOrganization>();
  const [selectedRepository, setSelectedRepository] = useState<GithubRepository>();

  const location = useLocation();
  const router = useRouter();

  const isMonorepoSetup = location.query?.mono === 'true';
  const hasDopSettings = Boolean(dopSettings?.length);
  const organizationOptions = useMemo(() => {
    return organizations.map(transformToOption);
  }, [organizations]);
  const repositoryOptions = useMemo(() => {
    return repositories.map(transformToOption);
  }, [repositories]);

  const fetchRepositories = useCallback(
    async (params: { organizationKey: string; page?: number; query?: string }) => {
      const { organizationKey, page = 1, query } = params;

      if (selectedDopSetting === undefined) {
        setIsInError(true);
        return;
      }

      setIsLoadingRepositories(true);

      try {
        const { paging, repositories } = await getGithubRepositories({
          almSetting: selectedDopSetting.key,
          organization: organizationKey,
          pageSize: REPOSITORY_PAGE_SIZE,
          page,
          query,
        });

        setRepositoryPaging(paging);
        setRepositories((prevRepositories) =>
          page === 1 ? repositories : [...prevRepositories, ...repositories],
        );
      } catch (_) {
        setRepositoryPaging({ pageIndex: 1, pageSize: REPOSITORY_PAGE_SIZE, total: 0 });
        setRepositories([]);
      } finally {
        setIsLoadingRepositories(false);
      }
    },
    [selectedDopSetting],
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
      fetchRepositories({
        organizationKey: selectedOrganization.key,
        page: repositoryPaging.pageIndex + 1,
        query: searchQuery,
      });
    }
  }, [fetchRepositories, repositoryPaging.pageIndex, searchQuery, selectedOrganization]);

  const handleSelectOrganization = useCallback(
    (organizationKey: string) => {
      setSearchQuery('');
      setSelectedOrganization(organizations.find(({ key }) => key === organizationKey));
      fetchRepositories({ organizationKey });
    },
    [fetchRepositories, organizations],
  );

  const handleSelectRepository = useCallback(
    (repositoryIdentifier: string) => {
      setSelectedRepository(repositories.find(({ key }) => key === repositoryIdentifier));
    },
    [repositories],
  );

  const authenticateToGithub = useCallback(async () => {
    try {
      await redirectToGithub({ isMonorepoSetup, selectedDopSetting });
    } catch {
      setIsInError(true);
    }
  }, [isMonorepoSetup, selectedDopSetting]);

  const onSelectDopSetting = useCallback((setting: DopSetting | undefined) => {
    setSelectedDopSetting(setting);
    setOrganizations([]);
    setRepositories([]);
    setSearchQuery('');
  }, []);

  const onSelectedAlmInstanceChange = useCallback(
    (instance: AlmSettingsInstance) => {
      onSelectDopSetting(dopSettings.find((dopSetting) => dopSetting.key === instance.key));
    },
    [dopSettings, onSelectDopSetting],
  );

  useEffect(() => {
    const selectedDopSettingId = location.query?.dopSetting;
    if (selectedDopSettingId !== undefined) {
      const selectedDopSetting = dopSettings.find(({ id }) => id === selectedDopSettingId);

      if (selectedDopSetting) {
        setSelectedDopSetting(selectedDopSetting);
      }

      return;
    }

    if (dopSettings.length > 1) {
      setSelectedDopSetting(undefined);
      return;
    }

    setSelectedDopSetting(dopSettings[0]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasDopSettings]);

  useEffect(() => {
    if (selectedDopSetting?.url === undefined) {
      setIsInError(true);
      return;
    }
    setIsInError(false);

    const code = location.query?.code;
    if (code === undefined) {
      authenticateToGithub().catch(() => {
        setIsInError(true);
      });
    } else {
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDopSetting]);

  useEffect(() => {
    repositorySearchDebounceId.current = setTimeout(() => {
      if (selectedOrganization) {
        fetchRepositories({
          organizationKey: selectedOrganization.key,
          query: searchQuery,
        });
      }
    }, REPOSITORY_SEARCH_DEBOUNCE_TIME);

    return () => {
      clearTimeout(repositorySearchDebounceId.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchQuery]);

  return isMonorepoSetup ? (
    <MonorepoProjectCreate
      dopSettings={dopSettings}
      canAdmin={canAdmin}
      error={isInError}
      loadingBindings={isLoadingBindings}
      loadingOrganizations={isLoadingOrganizations}
      loadingRepositories={isLoadingRepositories}
      onProjectSetupDone={onProjectSetupDone}
      onSearchRepositories={setSearchQuery}
      onSelectDopSetting={onSelectDopSetting}
      onSelectOrganization={handleSelectOrganization}
      onSelectRepository={handleSelectRepository}
      organizationOptions={organizationOptions}
      repositoryOptions={repositoryOptions}
      repositorySearchQuery={searchQuery}
      selectedDopSetting={selectedDopSetting}
      selectedOrganization={selectedOrganization && transformToOption(selectedOrganization)}
      selectedRepository={selectedRepository && transformToOption(selectedRepository)}
    />
  ) : (
    <GitHubProjectCreateRenderer
      almInstances={dopSettings.map(({ key, type, url }) => ({
        alm: type,
        key,
        url,
      }))}
      canAdmin={canAdmin}
      error={isInError}
      loadingBindings={isLoadingBindings}
      loadingOrganizations={isLoadingOrganizations}
      loadingRepositories={isLoadingRepositories}
      onImportRepository={handleImportRepository}
      onLoadMore={handleLoadMore}
      onSearch={setSearchQuery}
      onSelectedAlmInstanceChange={onSelectedAlmInstanceChange}
      onSelectOrganization={handleSelectOrganization}
      organizations={organizations}
      repositories={repositories}
      repositoryPaging={repositoryPaging}
      searchQuery={searchQuery}
      selectedAlmInstance={
        selectedDopSetting && {
          alm: selectedDopSetting.type,
          key: selectedDopSetting.key,
          url: selectedDopSetting.url,
        }
      }
      selectedOrganization={selectedOrganization}
    />
  );
}

function transformToOption({
  key,
  name,
}: GithubOrganization | GithubRepository): LabelValueSelectOption {
  return { value: key, label: name };
}
