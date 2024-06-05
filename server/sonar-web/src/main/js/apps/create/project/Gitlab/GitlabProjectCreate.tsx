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
import React, { useCallback, useMemo } from 'react';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import { getGitlabProjects } from '../../../../api/alm-integrations';
import { GitlabProject } from '../../../../types/alm-integration';
import { AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { ImportProjectParam } from '../CreateProjectPage';
import { REPOSITORY_PAGE_SIZE } from '../constants';
import MonorepoProjectCreate from '../monorepo/MonorepoProjectCreate';
import { CreateProjectModes } from '../types';
import { useProjectCreate } from '../useProjectCreate';
import { useRepositorySearch } from '../useRepositorySearch';
import GitlabPersonalAccessTokenForm from './GItlabPersonalAccessTokenForm';
import GitlabProjectCreateRenderer from './GitlabProjectCreateRenderer';

interface Props {
  dopSettings: DopSetting[];
  isLoadingBindings: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
}

export default function GitlabProjectCreate(props: Readonly<Props>) {
  const { dopSettings, isLoadingBindings, onProjectSetupDone } = props;

  const {
    almInstances,
    handlePersonalAccessTokenCreated,
    handleSelectRepository,
    isInitialized,
    isLoadingRepositories,
    isMonorepoSetup,
    onSelectedAlmInstanceChange,
    onSelectDopSetting,
    projectsPaging,
    repositories,
    resetPersonalAccessToken,
    searchQuery,
    selectedAlmInstance,
    selectedDopSetting,
    selectedRepository,
    setIsInitialized,
    setIsLoadingRepositories,
    setProjectsPaging,
    setRepositories,
    setResetPersonalAccessToken,
    setSearchQuery,
    setShowPersonalAccessTokenForm,
    showPersonalAccessTokenForm,
  } = useProjectCreate<GitlabProject, GitlabProject[], undefined>(
    AlmKeys.GitLab,
    dopSettings,
    ({ id }) => id,
  );

  const location = useLocation();

  const repositoryOptions = useMemo(() => {
    return repositories?.map(transformToOption);
  }, [repositories]);

  const fetchRepositories = useCallback(
    (_orgKey?: string, query = '', pageIndex = 1, more = false) => {
      if (showPersonalAccessTokenForm || !selectedDopSetting) {
        return Promise.resolve();
      }

      setIsLoadingRepositories(true);

      // eslint-disable-next-line local-rules/no-api-imports
      return getGitlabProjects({
        almSetting: selectedDopSetting.key,
        page: pageIndex,
        pageSize: REPOSITORY_PAGE_SIZE,
        query,
      })
        .then((result) => {
          if (result?.projects) {
            setProjectsPaging(result.projectsPaging);
            setRepositories(
              more && repositories && repositories.length > 0
                ? [...repositories, ...result.projects]
                : result.projects,
            );
            setIsInitialized(true);
          }
        })
        .finally(() => {
          setIsLoadingRepositories(false);
        })
        .catch(() => {
          setResetPersonalAccessToken(true);
          setShowPersonalAccessTokenForm(true);
          setIsLoadingRepositories(false);
        });
    },
    [
      repositories,
      selectedDopSetting,
      setIsInitialized,
      setIsLoadingRepositories,
      setProjectsPaging,
      setRepositories,
      setResetPersonalAccessToken,
      setShowPersonalAccessTokenForm,
      showPersonalAccessTokenForm,
    ],
  );

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

  const handleLoadMore = useCallback(() => {
    fetchRepositories(undefined, searchQuery, projectsPaging.pageIndex + 1, true);
  }, [fetchRepositories, projectsPaging, searchQuery]);

  const { onSearch } = useRepositorySearch(
    AlmKeys.GitLab,
    fetchRepositories,
    isInitialized,
    selectedDopSetting,
    undefined,
    setSearchQuery,
    showPersonalAccessTokenForm,
  );

  return isMonorepoSetup ? (
    <MonorepoProjectCreate
      dopSettings={dopSettings}
      error={false}
      loadingBindings={isLoadingBindings}
      loadingOrganizations={false}
      loadingRepositories={isLoadingRepositories}
      onProjectSetupDone={onProjectSetupDone}
      onSearchRepositories={onSearch}
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
      almInstances={almInstances}
      loading={isLoadingRepositories || isLoadingBindings}
      onImport={handleImportRepository}
      onLoadMore={handleLoadMore}
      onPersonalAccessTokenCreated={handlePersonalAccessTokenCreated}
      onSearch={onSearch}
      onSelectedAlmInstanceChange={onSelectedAlmInstanceChange}
      projects={repositories}
      projectsPaging={projectsPaging}
      resetPat={resetPersonalAccessToken || Boolean(location.query.resetPat)}
      searchQuery={searchQuery}
      selectedAlmInstance={selectedAlmInstance}
      showPersonalAccessTokenForm={showPersonalAccessTokenForm || Boolean(location.query.resetPat)}
    />
  );
}

function transformToOption({ id, name }: GitlabProject): LabelValueSelectOption {
  return { value: id, label: name };
}
