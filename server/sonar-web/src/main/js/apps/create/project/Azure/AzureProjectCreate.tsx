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
import { GroupBase } from 'react-select';
import {
  getAzureProjects,
  getAzureRepositories,
  searchAzureRepositories,
} from '../../../../api/alm-integrations';
import { useLocation, useRouter } from '../../../../components/hoc/withRouter';
import { AzureProject, AzureRepository } from '../../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { Dict } from '../../../../types/types';
import { ImportProjectParam } from '../CreateProjectPage';
import MonorepoProjectCreate from '../monorepo/MonorepoProjectCreate';
import { CreateProjectModes } from '../types';
import AzurePersonalAccessTokenForm from './AzurePersonalAccessTokenForm';
import AzureCreateProjectRenderer from './AzureProjectCreateRenderer';

interface Props {
  dopSettings: DopSetting[];
  isLoadingBindings: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
}

export default function AzureProjectCreate(props: Readonly<Props>) {
  const { dopSettings, isLoadingBindings, onProjectSetupDone } = props;
  const [isLoading, setIsLoading] = useState(false);
  const [loadingRepositories, setLoadingRepositories] = useState<Dict<boolean>>({});
  const [isSearching, setIsSearching] = useState(false);
  const [projects, setProjects] = useState<AzureProject[] | undefined>();
  const [repositories, setRepositories] = useState<Dict<AzureRepository[]>>({});
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [searchResults, setSearchResults] = useState<AzureRepository[] | undefined>();
  const [selectedDopSetting, setSelectedDopSetting] = useState<DopSetting | undefined>();
  const [selectedRepository, setSelectedRepository] = useState<AzureRepository>();
  const [showPersonalAccessTokenForm, setShowPersonalAccessTokenForm] = useState(true);

  const location = useLocation();
  const router = useRouter();

  const almInstances = useMemo(
    () =>
      dopSettings?.map((dopSetting) => ({
        alm: dopSetting.type,
        key: dopSetting.key,
        url: dopSetting.url,
      })) ?? [],
    [dopSettings],
  );
  const isMonorepoSetup = location.query?.mono === 'true';
  const hasDopSettings = Boolean(dopSettings?.length);
  const selectedAlmInstance = useMemo(
    () =>
      selectedDopSetting && {
        alm: selectedDopSetting.type,
        key: selectedDopSetting.key,
        url: selectedDopSetting.url,
      },
    [selectedDopSetting],
  );
  const repositoryOptions = useMemo(
    () => transformToOptions(projects ?? [], repositories),
    [projects, repositories],
  );

  const cleanUrl = useCallback(() => {
    delete location.query.resetPat;
    router.replace(location);
  }, [location, router]);

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

    setIsLoading(true);
    let projects: AzureProject[] | undefined;
    try {
      projects = await fetchAzureProjects();
    } catch (_) {
      setShowPersonalAccessTokenForm(true);
      setIsLoading(false);
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
    setIsLoading(false);
  }, [fetchAzureProjects, fetchAzureRepositories, isMonorepoSetup, showPersonalAccessTokenForm]);

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
    [fetchAzureRepositories, searchResults],
  );

  const handlePersonalAccessTokenCreate = useCallback(() => {
    cleanUrl();
    setShowPersonalAccessTokenForm(false);
  }, [cleanUrl]);

  const handleSearchRepositories = useCallback(
    async (searchQuery: string) => {
      if (!selectedDopSetting) {
        return;
      }

      if (searchQuery.length === 0) {
        setSearchResults(undefined);
        setSearchQuery('');
        return;
      }

      setIsSearching(true);

      const searchResults: AzureRepository[] = await searchAzureRepositories(
        selectedDopSetting.key,
        searchQuery,
      )
        .then(({ repositories }) => repositories)
        .catch(() => []);

      setIsSearching(false);
      setSearchQuery(searchQuery);
      setSearchResults(searchResults);
    },
    [selectedDopSetting],
  );

  const handleSelectRepository = useCallback(
    (repositoryKey: string) => {
      setSelectedRepository(
        Object.values(repositories)
          .flat()
          .find(({ name }) => name === repositoryKey),
      );
    },
    [repositories],
  );

  const onSelectedAlmInstanceChange = useCallback(
    (almInstance: AlmSettingsInstance) => {
      setSelectedDopSetting(dopSettings?.find((dopSetting) => dopSetting.key === almInstance.key));
    },
    [dopSettings],
  );

  useEffect(() => {
    setSelectedDopSetting(dopSettings?.[0]);
    // We want to update this value only when the list of DOP settings changes from empty to not-empty (or vice-versa)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasDopSettings]);

  useEffect(() => {
    setSearchResults(undefined);
    setSearchQuery('');
    setShowPersonalAccessTokenForm(true);
  }, [isMonorepoSetup, selectedDopSetting]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return isMonorepoSetup ? (
    <MonorepoProjectCreate
      dopSettings={dopSettings}
      error={false}
      loadingBindings={isLoadingBindings}
      loadingOrganizations={false}
      loadingRepositories={isLoading}
      onProjectSetupDone={handleMonorepoSetupDone}
      onSearchRepositories={setSearchQuery}
      onSelectDopSetting={setSelectedDopSetting}
      onSelectRepository={handleSelectRepository}
      personalAccessTokenComponent={
        !isLoading &&
        selectedAlmInstance && (
          <AzurePersonalAccessTokenForm
            almSetting={selectedAlmInstance}
            onPersonalAccessTokenCreate={handlePersonalAccessTokenCreate}
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
      loading={isLoading || isLoadingBindings}
      loadingRepositories={loadingRepositories}
      onImportRepository={handleImportRepository}
      onOpenProject={handleOpenProject}
      onPersonalAccessTokenCreate={handlePersonalAccessTokenCreate}
      onSearch={handleSearchRepositories}
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
  repositories: Dict<AzureRepository[]>,
): Array<GroupBase<LabelValueSelectOption<string>>> {
  return projects.map(({ name: projectName }) => ({
    label: projectName,
    options: repositories[projectName]?.map(transformToOption) ?? [],
  }));
}

function transformToOption({ name }: AzureRepository): LabelValueSelectOption<string> {
  return { value: name, label: name };
}
