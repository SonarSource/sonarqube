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

import { useCallback, useMemo } from 'react';
import { GroupBase } from 'react-select';
import { LabelValueSelectOption } from '~design-system';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import {
  getBitbucketServerProjects,
  getBitbucketServerRepositories,
  searchForBitbucketServerRepositories,
} from '../../../../api/alm-integrations';
import { BitbucketProject, BitbucketRepository } from '../../../../types/alm-integration';
import { AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { Dict } from '../../../../types/types';
import { ImportProjectParam } from '../CreateProjectPage';
import MonorepoProjectCreate from '../monorepo/MonorepoProjectCreate';
import { CreateProjectModes } from '../types';
import { useProjectCreate } from '../useProjectCreate';
import { useProjectRepositorySearch } from '../useProjectRepositorySearch';
import BitbucketCreateProjectRenderer from './BitbucketProjectCreateRenderer';
import BitbucketServerPersonalAccessTokenForm from './BitbucketServerPersonalAccessTokenForm';

interface Props {
  dopSettings: DopSetting[];
  isLoadingBindings: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
}

export default function BitbucketProjectCreate({
  dopSettings,
  isLoadingBindings,
  onProjectSetupDone,
}: Readonly<Props>) {
  const {
    almInstances,
    handlePersonalAccessTokenCreated,
    handleSelectRepository: defaultRepositorySelect,
    isLoadingRepositories,
    isMonorepoSetup,
    onSelectedAlmInstanceChange,
    organizations: projects,
    repositories,
    resetPersonalAccessToken,
    searchQuery,
    selectedAlmInstance,
    selectedDopSetting,
    selectedRepository,
    setIsLoadingRepositories,
    setOrganizations: setProjects,
    setRepositories,
    setSearchQuery,
    setSelectedDopSetting,
    setSelectedRepository,
    setShowPersonalAccessTokenForm,
    showPersonalAccessTokenForm,
  } = useProjectCreate<BitbucketRepository, Dict<BitbucketRepository[]>, BitbucketProject>(
    AlmKeys.BitbucketServer,
    dopSettings,
    ({ slug }) => slug,
  );

  const location = useLocation();

  const fetchBitbucketProjects = useCallback((): Promise<BitbucketProject[] | undefined> => {
    if (!selectedDopSetting) {
      return Promise.resolve(undefined);
    }

    return getBitbucketServerProjects(selectedDopSetting.key).then(({ projects }) => projects);
  }, [selectedDopSetting]);

  const fetchBitbucketRepositories = useCallback(
    (projects: BitbucketProject[]): Promise<Dict<BitbucketRepository[]> | undefined> => {
      if (!selectedDopSetting) {
        return Promise.resolve(undefined);
      }

      return Promise.all(
        projects.map((p) => {
          return getBitbucketServerRepositories(selectedDopSetting.key, p.name).then(
            ({ repositories }) => {
              // Because the WS uses the project name rather than its key to find
              // repositories, we can match more repositories than we expect. For
              // example, p.name = "A1" would find repositories for projects "A1",
              // "A10", "A11", etc. This is a limitation of BBS. To make sure we
              // don't display incorrect information, filter on the project key.
              const filteredRepositories = repositories.filter((r) => r.projectKey === p.key);

              return {
                repositories: filteredRepositories,
                projectKey: p.key,
              };
            },
          );
        }),
      ).then((results) => {
        return results.reduce((acc: Dict<BitbucketRepository[]>, { projectKey, repositories }) => {
          return { ...acc, [projectKey]: repositories };
        }, {});
      });
    },
    [selectedDopSetting],
  );

  const handleImportRepository = useCallback(
    (selectedRepository: BitbucketRepository) => {
      if (selectedDopSetting) {
        onProjectSetupDone({
          creationMode: CreateProjectModes.BitbucketServer,
          almSetting: selectedDopSetting.key,
          monorepo: false,
          projects: [
            {
              projectKey: selectedRepository.projectKey,
              repositorySlug: selectedRepository.slug,
            },
          ],
        });
      }
    },
    [onProjectSetupDone, selectedDopSetting],
  );

  const handleMonorepoSetupDone = useCallback(
    (monorepoSetup: ImportProjectParam) => {
      const bitbucketMonorepoSetup = {
        ...monorepoSetup,
        projectIdentifier: selectedRepository?.projectKey,
      };

      onProjectSetupDone(bitbucketMonorepoSetup);
    },
    [onProjectSetupDone, selectedRepository?.projectKey],
  );

  const fetchData = useCallback(async () => {
    if (!showPersonalAccessTokenForm) {
      setIsLoadingRepositories(true);
      const projects = await fetchBitbucketProjects().catch(() => undefined);

      let projectRepositories;
      if (projects && projects.length > 0) {
        projectRepositories = await fetchBitbucketRepositories(projects).catch(() => undefined);
      }

      setProjects(projects ?? []);
      setRepositories(projectRepositories ?? {});
      setIsLoadingRepositories(false);
    }
  }, [
    fetchBitbucketProjects,
    fetchBitbucketRepositories,
    showPersonalAccessTokenForm,
    setIsLoadingRepositories,
    setProjects,
    setRepositories,
  ]);

  const { isSearching, onSearch, onSelectRepository, searchResults } =
    useProjectRepositorySearch<BitbucketRepository>({
      defaultRepositorySelect,
      fetchData,
      fetchSearchResults: (query: string, dopKey: string) =>
        searchForBitbucketServerRepositories(dopKey, query),
      getRepositoryKey: ({ slug }) => slug,
      isMonorepoSetup,
      selectedDopSetting,
      setSearchQuery,
      setSelectedRepository,
      setShowPersonalAccessTokenForm,
    });

  const repositoryOptions = useMemo(() => {
    if (searchResults) {
      const dict = projects?.reduce((acc: Dict<BitbucketRepository[]>, { key }) => {
        return { ...acc, [key]: searchResults?.filter((o) => o.projectKey === key) };
      }, {});
      return transformToOptions(projects ?? [], dict);
    }

    return transformToOptions(projects ?? [], repositories);
  }, [projects, repositories, searchResults]);

  return isMonorepoSetup ? (
    <MonorepoProjectCreate
      dopSettings={dopSettings}
      error={false}
      loadingBindings={isLoadingBindings}
      loadingOrganizations={false}
      loadingRepositories={isLoadingRepositories}
      onProjectSetupDone={handleMonorepoSetupDone}
      onSearchRepositories={onSearch}
      onSelectDopSetting={setSelectedDopSetting}
      onSelectRepository={onSelectRepository}
      personalAccessTokenComponent={
        !isLoadingRepositories &&
        selectedDopSetting && (
          <BitbucketServerPersonalAccessTokenForm
            almSetting={selectedDopSetting}
            onPersonalAccessTokenCreated={handlePersonalAccessTokenCreated}
            resetPat={resetPersonalAccessToken}
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
    <BitbucketCreateProjectRenderer
      almInstances={almInstances}
      isLoading={isLoadingRepositories || isLoadingBindings}
      onImportRepository={handleImportRepository}
      onPersonalAccessTokenCreated={handlePersonalAccessTokenCreated}
      onSearch={onSearch}
      onSelectedAlmInstanceChange={onSelectedAlmInstanceChange}
      projectRepositories={repositories}
      projects={projects}
      resetPat={Boolean(location.query.resetPat)}
      searchResults={searchResults}
      searching={isSearching}
      selectedAlmInstance={selectedAlmInstance}
      showPersonalAccessTokenForm={showPersonalAccessTokenForm || Boolean(location.query.resetPat)}
    />
  );
}

function transformToOptions(
  projects: BitbucketProject[],
  repositories?: Dict<BitbucketRepository[]>,
): Array<GroupBase<LabelValueSelectOption<string>>> {
  return projects.map(({ name, key }) => ({
    label: name,
    options: repositories?.[key] !== undefined ? repositories[key].map(transformToOption) : [],
  }));
}

function transformToOption({ name, slug }: BitbucketRepository): LabelValueSelectOption<string> {
  return { value: slug, label: name };
}
