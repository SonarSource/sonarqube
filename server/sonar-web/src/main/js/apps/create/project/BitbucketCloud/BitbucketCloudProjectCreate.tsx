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

import { useCallback, useMemo, useState } from 'react';
import { LabelValueSelectOption } from '~design-system';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import { searchForBitbucketCloudRepositories } from '../../../../api/alm-integrations';
import { BitbucketCloudRepository } from '../../../../types/alm-integration';
import { AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { ImportProjectParam } from '../CreateProjectPage';
import { REPOSITORY_PAGE_SIZE } from '../constants';
import MonorepoProjectCreate from '../monorepo/MonorepoProjectCreate';
import { CreateProjectModes } from '../types';
import { useProjectCreate } from '../useProjectCreate';
import { useRepositorySearch } from '../useRepositorySearch';
import BitbucketCloudPersonalAccessTokenForm from './BitbucketCloudPersonalAccessTokenForm';
import BitbucketCloudProjectCreateRenderer from './BitbucketCloudProjectCreateRender';

interface Props {
  dopSettings: DopSetting[];
  isLoadingBindings: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
}

export default function BitbucketCloudProjectCreate(props: Readonly<Props>) {
  const { dopSettings, isLoadingBindings, onProjectSetupDone } = props;

  const [isLastPage, setIsLastPage] = useState<boolean>(true);
  const [projectsPaging, setProjectsPaging] = useState<{ pageIndex: number; pageSize: number }>({
    pageIndex: 1,
    pageSize: REPOSITORY_PAGE_SIZE,
  });

  const {
    almInstances,
    handlePersonalAccessTokenCreated,
    handleSelectRepository,
    isInitialized,
    isLoadingRepositories,
    isLoadingMoreRepositories,
    isMonorepoSetup,
    onSelectedAlmInstanceChange,
    onSelectDopSetting,
    repositories,
    resetLoading,
    resetPersonalAccessToken,
    searchQuery,
    selectedAlmInstance,
    selectedDopSetting,
    selectedRepository,
    setIsInitialized,
    setRepositories,
    setResetPersonalAccessToken,
    setSearchQuery,
    setShowPersonalAccessTokenForm,
    showPersonalAccessTokenForm,
  } = useProjectCreate<BitbucketCloudRepository, BitbucketCloudRepository[], undefined>(
    AlmKeys.BitbucketCloud,
    dopSettings,
    ({ slug }) => slug,
  );

  const location = useLocation();
  const repositoryOptions = useMemo(() => repositories?.map(transformToOption), [repositories]);

  const fetchRepositories = useCallback(
    (_orgKey?: string, query = '', pageIndex = 1, more = false) => {
      if (!selectedDopSetting || showPersonalAccessTokenForm) {
        return Promise.resolve();
      }

      resetLoading(true, more);

      // eslint-disable-next-line local-rules/no-api-imports
      return searchForBitbucketCloudRepositories(
        selectedDopSetting.key,
        query,
        REPOSITORY_PAGE_SIZE,
        pageIndex,
      )
        .then((result) => {
          resetLoading(false, more);

          if (result) {
            setIsLastPage(result.isLastPage);
            setIsInitialized(true);
          }

          if (result?.repositories) {
            setRepositories(
              more && repositories && repositories.length > 0
                ? [...repositories, ...result.repositories]
                : result.repositories,
            );
          }
        })
        .catch(() => {
          resetLoading(false, more);
          setResetPersonalAccessToken(true);
          setShowPersonalAccessTokenForm(true);
        });
    },
    [
      repositories,
      resetLoading,
      selectedDopSetting,
      showPersonalAccessTokenForm,
      setIsInitialized,
      setIsLastPage,
      setRepositories,
      setResetPersonalAccessToken,
      setShowPersonalAccessTokenForm,
    ],
  );

  const handleLoadMore = useCallback(() => {
    const page = projectsPaging.pageIndex + 1;
    setProjectsPaging((paging) => ({
      pageIndex: page,
      pageSize: paging.pageSize,
    }));

    fetchRepositories(undefined, searchQuery, page, true);
  }, [fetchRepositories, projectsPaging, searchQuery, setProjectsPaging]);

  const handleImportRepository = useCallback(
    (repositorySlug: string) => {
      if (selectedDopSetting) {
        onProjectSetupDone({
          creationMode: CreateProjectModes.BitbucketCloud,
          almSetting: selectedDopSetting.key,
          monorepo: false,
          projects: [{ repositorySlug }],
        });
      }
    },
    [onProjectSetupDone, selectedDopSetting],
  );

  const { isSearching, onSearch } = useRepositorySearch(
    AlmKeys.BitbucketCloud,
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
          <BitbucketCloudPersonalAccessTokenForm
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
    <BitbucketCloudProjectCreateRenderer
      almInstances={almInstances}
      isLastPage={isLastPage}
      loadingMore={isLoadingMoreRepositories}
      loading={isLoadingRepositories || isLoadingBindings}
      onImport={handleImportRepository}
      onLoadMore={handleLoadMore}
      onPersonalAccessTokenCreated={handlePersonalAccessTokenCreated}
      onSearch={onSearch}
      onSelectedAlmInstanceChange={onSelectedAlmInstanceChange}
      repositories={repositories}
      resetPat={resetPersonalAccessToken || Boolean(location.query.resetPat)}
      searching={isSearching}
      searchQuery={searchQuery}
      selectedAlmInstance={selectedAlmInstance}
      showPersonalAccessTokenForm={showPersonalAccessTokenForm || Boolean(location.query.resetPat)}
    />
  );
}

function transformToOption({ name, slug }: BitbucketCloudRepository): LabelValueSelectOption {
  return { value: slug, label: name };
}
