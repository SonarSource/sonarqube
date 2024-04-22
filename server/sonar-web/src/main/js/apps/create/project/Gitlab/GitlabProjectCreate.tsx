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
import { orderBy } from 'lodash';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getGitlabProjects } from '../../../../api/alm-integrations';
import { useLocation, useRouter } from '../../../../components/hoc/withRouter';
import { GitlabProject } from '../../../../types/alm-integration';
import { AlmInstanceBase } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { Paging } from '../../../../types/types';
import { ImportProjectParam } from '../CreateProjectPage';
import MonorepoProjectCreate from '../monorepo/MonorepoProjectCreate';
import { CreateProjectModes } from '../types';
import GitlabPersonalAccessTokenForm from './GItlabPersonalAccessTokenForm';
import GitlabProjectCreateRenderer from './GitlabProjectCreateRenderer';

interface Props {
  canAdmin: boolean;
  isLoadingBindings: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
  dopSettings: DopSetting[];
}

const REPOSITORY_PAGE_SIZE = 50;
const REPOSITORY_SEARCH_DEBOUNCE_TIME = 250;

export default function GitlabProjectCreate(props: Readonly<Props>) {
  const { canAdmin, dopSettings, isLoadingBindings, onProjectSetupDone } = props;

  const repositorySearchDebounceId = useRef<NodeJS.Timeout | undefined>();

  const [isLoadingRepositories, setIsLoadingRepositories] = useState(false);
  const [repositories, setRepositories] = useState<GitlabProject[]>([]);
  const [repositoryPaging, setRepositoryPaging] = useState<Paging>({
    pageSize: REPOSITORY_PAGE_SIZE,
    total: 0,
    pageIndex: 1,
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDopSetting, setSelectedDopSetting] = useState<DopSetting>();
  const [selectedRepository, setSelectedRepository] = useState<GitlabProject>();
  const [resetPersonalAccessToken, setResetPersonalAccessToken] = useState<boolean>(false);
  const [showPersonalAccessTokenForm, setShowPersonalAccessTokenForm] = useState<boolean>(true);

  const location = useLocation();
  const router = useRouter();

  const isMonorepoSetup = location.query?.mono === 'true';
  const hasDopSettings = useMemo(() => {
    if (dopSettings === undefined) {
      return false;
    }

    return dopSettings.length > 0;
  }, [dopSettings]);
  const repositoryOptions = useMemo(() => {
    return repositories.map(transformToOption);
  }, [repositories]);

  const fetchProjects = useCallback(
    (pageIndex = 1, query?: string) => {
      if (!selectedDopSetting) {
        return Promise.resolve(undefined);
      }

      // eslint-disable-next-line local-rules/no-api-imports
      return getGitlabProjects({
        almSetting: selectedDopSetting.key,
        page: pageIndex,
        pageSize: REPOSITORY_PAGE_SIZE,
        query,
      });
    },
    [selectedDopSetting],
  );

  const fetchInitialData = useCallback(() => {
    if (!showPersonalAccessTokenForm) {
      setIsLoadingRepositories(true);

      fetchProjects()
        .then((result) => {
          if (result?.projects) {
            setIsLoadingRepositories(false);
            setRepositories(
              isMonorepoSetup
                ? orderBy(result.projects, [(res) => res.name.toLowerCase()], ['asc'])
                : result.projects,
            );
            setRepositoryPaging(result.projectsPaging);
          } else {
            setIsLoadingRepositories(false);
          }
        })
        .catch(() => {
          setResetPersonalAccessToken(true);
          setShowPersonalAccessTokenForm(true);
        });
    }
  }, [fetchProjects, isMonorepoSetup, showPersonalAccessTokenForm]);

  const cleanUrl = useCallback(() => {
    delete location.query.resetPat;
    router.replace(location);
  }, [location, router]);

  const handlePersonalAccessTokenCreated = useCallback(() => {
    cleanUrl();
    setShowPersonalAccessTokenForm(false);
    setResetPersonalAccessToken(false);
    fetchInitialData();
  }, [cleanUrl, fetchInitialData]);

  const handleImportRepository = useCallback(
    (repoKeys: string[]) => {
      if (selectedDopSetting && repoKeys.length > 0) {
        onProjectSetupDone({
          almSetting: selectedDopSetting.key,
          creationMode: CreateProjectModes.GitLab,
          monorepo: false,
          projects: repoKeys.map((repoKeys) => ({ gitlabProjectId: repoKeys })),
        });
      }
    },
    [onProjectSetupDone, selectedDopSetting],
  );

  const handleLoadMore = useCallback(async () => {
    const result = await fetchProjects(repositoryPaging.pageIndex + 1, searchQuery);
    if (result?.projects) {
      setRepositoryPaging(result ? result.projectsPaging : repositoryPaging);
      setRepositories(result ? [...repositories, ...result.projects] : repositories);
    }
  }, [fetchProjects, repositories, repositoryPaging, searchQuery]);

  const handleSelectRepository = useCallback(
    (repositoryKey: string) => {
      setSelectedRepository(repositories.find(({ id }) => id === repositoryKey));
    },
    [repositories],
  );

  const onSelectDopSetting = useCallback((setting: DopSetting | undefined) => {
    setSelectedDopSetting(setting);
    setShowPersonalAccessTokenForm(true);
    setRepositories([]);
    setSearchQuery('');
  }, []);

  const onSelectedAlmInstanceChange = useCallback(
    (instance: AlmInstanceBase) => {
      onSelectDopSetting(dopSettings.find((dopSetting) => dopSetting.key === instance.key));
    },
    [dopSettings, onSelectDopSetting],
  );

  useEffect(() => {
    if (dopSettings.length > 0) {
      setSelectedDopSetting(dopSettings[0]);
      return;
    }

    setSelectedDopSetting(undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasDopSettings]);

  useEffect(() => {
    if (selectedDopSetting) {
      fetchInitialData();
    }
  }, [fetchInitialData, selectedDopSetting]);

  useEffect(() => {
    repositorySearchDebounceId.current = setTimeout(async () => {
      const result = await fetchProjects(1, searchQuery);
      if (result?.projects) {
        setRepositories(orderBy(result.projects, [(res) => res.name.toLowerCase()], ['asc']));
        setRepositoryPaging(result.projectsPaging);
      }
    }, REPOSITORY_SEARCH_DEBOUNCE_TIME);

    return () => {
      clearTimeout(repositorySearchDebounceId.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchQuery]);

  return isMonorepoSetup ? (
    <MonorepoProjectCreate
      canAdmin={canAdmin}
      dopSettings={dopSettings}
      error={false}
      loadingBindings={isLoadingBindings}
      loadingOrganizations={false}
      loadingRepositories={isLoadingRepositories}
      onProjectSetupDone={onProjectSetupDone}
      onSearchRepositories={setSearchQuery}
      onSelectDopSetting={onSelectDopSetting}
      onSelectRepository={handleSelectRepository}
      personalAccessTokenComponent={
        !isLoadingRepositories &&
        selectedDopSetting && (
          <GitlabPersonalAccessTokenForm
            almSetting={selectedDopSetting}
            resetPat={resetPersonalAccessToken}
            onPersonalAccessTokenCreated={handlePersonalAccessTokenCreated}
          />
        )
      }
      repositoryOptions={repositoryOptions}
      repositorySearchQuery={searchQuery}
      selectedDopSetting={selectedDopSetting}
      selectedRepository={selectedRepository ? transformToOption(selectedRepository) : undefined}
      showPersonalAccessToken={showPersonalAccessTokenForm || Boolean(location.query.resetPat)}
    />
  ) : (
    <GitlabProjectCreateRenderer
      almInstances={dopSettings.map((dopSetting) => ({
        alm: dopSetting.type,
        key: dopSetting.key,
        url: dopSetting.url,
      }))}
      canAdmin={canAdmin}
      loading={isLoadingRepositories || isLoadingBindings}
      onImport={handleImportRepository}
      onLoadMore={handleLoadMore}
      onPersonalAccessTokenCreated={handlePersonalAccessTokenCreated}
      onSearch={setSearchQuery}
      onSelectedAlmInstanceChange={onSelectedAlmInstanceChange}
      projects={repositories}
      projectsPaging={repositoryPaging}
      resetPat={resetPersonalAccessToken || Boolean(location.query.resetPat)}
      searchQuery={searchQuery}
      selectedAlmInstance={
        selectedDopSetting && {
          alm: selectedDopSetting.type,
          key: selectedDopSetting.key,
          url: selectedDopSetting.url,
        }
      }
      showPersonalAccessTokenForm={showPersonalAccessTokenForm || Boolean(location.query.resetPat)}
    />
  );
}

function transformToOption({ id, name }: GitlabProject): LabelValueSelectOption<string> {
  return { value: id, label: name };
}
