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
import { GroupBase } from 'react-select';
import { LabelValueSelectOption } from '~design-system';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import {
  getAzureProjects,
  getAzureRepositories,
  searchAzureRepositories,
} from '../../../../api/alm-integrations';
import { AzureProject, AzureRepository } from '../../../../types/alm-integration';
import { AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { Dict } from '../../../../types/types';
import { ImportProjectParam } from '../CreateProjectPage';
import MonorepoProjectCreate from '../monorepo/MonorepoProjectCreate';
import { CreateProjectModes } from '../types';
import { useProjectCreate } from '../useProjectCreate';
import { useProjectRepositorySearch } from '../useProjectRepositorySearch';
import AzurePersonalAccessTokenForm from './AzurePersonalAccessTokenForm';
import AzureCreateProjectRenderer from './AzureProjectCreateRenderer';

interface Props {
  dopSettings: DopSetting[];
  isLoadingBindings: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
}

export default function AzureProjectCreate({
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
    searchQuery,
    selectedAlmInstance,
    selectedDopSetting,
    selectedRepository,
    setSearchQuery,
    setIsLoadingRepositories,
    setOrganizations: setProjects,
    setRepositories,
    setSelectedDopSetting,
    setSelectedRepository,
    setShowPersonalAccessTokenForm,
    showPersonalAccessTokenForm,
  } = useProjectCreate<AzureRepository, Dict<AzureRepository[]>, AzureProject>(
    AlmKeys.Azure,
    dopSettings,
    ({ name }) => name,
  );

  const [loadingRepositories, setLoadingRepositories] = useState<Dict<boolean>>({});

  const location = useLocation();

  const fetchAzureProjects = useCallback(async (): Promise<AzureProject[] | undefined> => {
    if (selectedDopSetting === undefined) {
      return undefined;
    }

    const azureProjects = await getAzureProjects(selectedDopSetting.key);

    return azureProjects.projects;
  }, [selectedDopSetting]);

  const fetchAzureRepositories = useCallback(
    async (projectName: string): Promise<AzureRepository[]> => {
      if (!selectedDopSetting) {
        return [];
      }

      try {
        const azureRepositories = await getAzureRepositories(selectedDopSetting.key, projectName);
        return azureRepositories.repositories;
      } catch {
        return [];
      }
    },
    [selectedDopSetting],
  );

  const fetchData = useCallback(async () => {
    if (showPersonalAccessTokenForm) {
      return;
    }

    setIsLoadingRepositories(true);
    let projects: AzureProject[] | undefined;
    try {
      projects = await fetchAzureProjects();
    } catch (_) {
      setShowPersonalAccessTokenForm(true);
      setIsLoadingRepositories(false);
      return;
    }

    if (projects && projects.length > 0) {
      if (isMonorepoSetup) {
        // Load every projects repos if we're in monorepo setup
        projects.forEach(async (project) => {
          setLoadingRepositories((loadingRepositories) => ({
            ...loadingRepositories,
            [project.name]: true,
          }));

          try {
            const repos = await fetchAzureRepositories(project.name);
            setRepositories((repositories) => ({
              ...repositories,
              [project.name]: repos,
            }));
          } finally {
            setLoadingRepositories((loadingRepositories) => {
              loadingRepositories[project.name] = false;
              return { ...loadingRepositories };
            });
          }
        });
      } else {
        const firstProjectName = projects[0].name;

        setLoadingRepositories((loadingRepositories) => ({
          ...loadingRepositories,
          [firstProjectName]: true,
        }));

        const repos = await fetchAzureRepositories(firstProjectName);

        setLoadingRepositories((loadingRepositories) => {
          loadingRepositories[firstProjectName] = false;

          return { ...loadingRepositories };
        });
        setRepositories((repositories) => ({ ...repositories, [firstProjectName]: repos }));
      }
    }

    setProjects(projects);
    setIsLoadingRepositories(false);
  }, [
    fetchAzureProjects,
    fetchAzureRepositories,
    isMonorepoSetup,
    setIsLoadingRepositories,
    setProjects,
    setRepositories,
    setShowPersonalAccessTokenForm,
    showPersonalAccessTokenForm,
  ]);

  const handleImportRepository = useCallback(
    (selectedRepository: AzureRepository) => {
      if (selectedDopSetting !== undefined && selectedRepository !== undefined) {
        onProjectSetupDone({
          creationMode: CreateProjectModes.AzureDevOps,
          almSetting: selectedDopSetting.key,
          monorepo: false,
          projects: [
            {
              projectName: selectedRepository.projectName,
              repositoryName: selectedRepository.name,
            },
          ],
        });
      }
    },
    [onProjectSetupDone, selectedDopSetting],
  );

  const handleMonorepoSetupDone = useCallback(
    (monorepoSetup: ImportProjectParam) => {
      const azureMonorepoSetup = {
        ...monorepoSetup,
        projectIdentifier: selectedRepository?.projectName,
      };

      onProjectSetupDone(azureMonorepoSetup);
    },
    [onProjectSetupDone, selectedRepository?.projectName],
  );

  const { isSearching, onSearch, onSelectRepository, searchResults } =
    useProjectRepositorySearch<AzureRepository>({
      defaultRepositorySelect,
      fetchData,
      fetchSearchResults: (query: string, dopKey: string) => searchAzureRepositories(dopKey, query),
      getRepositoryKey: ({ name }) => name,
      isMonorepoSetup,
      selectedDopSetting,
      setSearchQuery,
      setSelectedRepository,
      setShowPersonalAccessTokenForm,
    });

  const handleOpenProject = useCallback(
    async (projectName: string) => {
      if (searchResults !== undefined) {
        return;
      }

      setLoadingRepositories((loadingRepositories) => ({
        ...loadingRepositories,
        [projectName]: true,
      }));

      const projectRepos = await fetchAzureRepositories(projectName);

      setLoadingRepositories((loadingRepositories) => ({
        ...loadingRepositories,
        [projectName]: false,
      }));
      setRepositories((repositories) => ({ ...repositories, [projectName]: projectRepos }));
    },
    [fetchAzureRepositories, searchResults, setRepositories],
  );

  const repositoryOptions = useMemo(() => {
    if (searchResults) {
      const dict = projects?.reduce((acc: Dict<AzureRepository[]>, { name }) => {
        return { ...acc, [name]: searchResults?.filter((o) => o.projectName === name) };
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
        selectedAlmInstance && (
          <AzurePersonalAccessTokenForm
            almSetting={selectedAlmInstance}
            onPersonalAccessTokenCreate={handlePersonalAccessTokenCreated}
            resetPat={Boolean(location.query.resetPat)}
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
    <AzureCreateProjectRenderer
      almInstances={almInstances}
      loading={isLoadingRepositories || isLoadingBindings}
      loadingRepositories={loadingRepositories}
      onImportRepository={handleImportRepository}
      onOpenProject={handleOpenProject}
      onPersonalAccessTokenCreate={handlePersonalAccessTokenCreated}
      onSearch={onSearch}
      onSelectedAlmInstanceChange={onSelectedAlmInstanceChange}
      projects={projects}
      repositories={repositories}
      resetPat={Boolean(location.query.resetPat)}
      searching={isSearching}
      searchResults={searchResults}
      searchQuery={searchQuery}
      selectedAlmInstance={selectedAlmInstance}
      showPersonalAccessTokenForm={showPersonalAccessTokenForm || Boolean(location.query.resetPat)}
    />
  );
}

function transformToOptions(
  projects: AzureProject[],
  repositories?: Dict<AzureRepository[]>,
): Array<GroupBase<LabelValueSelectOption>> {
  return projects.map(({ name: projectName }) => ({
    label: projectName,
    options:
      repositories?.[projectName] !== undefined
        ? repositories[projectName].map(transformToOption)
        : [],
  }));
}

function transformToOption({ name }: AzureRepository): LabelValueSelectOption {
  return { value: name, label: name };
}
