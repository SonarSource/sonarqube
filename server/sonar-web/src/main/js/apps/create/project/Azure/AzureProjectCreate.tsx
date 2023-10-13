/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import * as React from 'react';
import {
  getAzureProjects,
  getAzureRepositories,
  searchAzureRepositories,
} from '../../../../api/alm-integrations';
import { Location, Router } from '../../../../components/hoc/withRouter';
import { AzureProject, AzureRepository } from '../../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../../types/alm-settings';
import { Dict } from '../../../../types/types';
import { ImportProjectParam } from '../CreateProjectPage';
import { CreateProjectModes } from '../types';
import AzureCreateProjectRenderer from './AzureProjectCreateRenderer';

interface Props {
  canAdmin: boolean;
  loadingBindings: boolean;
  almInstances: AlmSettingsInstance[];
  location: Location;
  router: Router;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
}

interface State {
  loading: boolean;
  loadingRepositories: Dict<boolean>;
  projects?: AzureProject[];
  repositories: Dict<AzureRepository[]>;
  searching?: boolean;
  searchResults?: AzureRepository[];
  searchQuery?: string;
  selectedAlmInstance?: AlmSettingsInstance;
  showPersonalAccessTokenForm: boolean;
}

export default class AzureProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      selectedAlmInstance: props.almInstances[0],
      loading: false,
      showPersonalAccessTokenForm: true,
      loadingRepositories: {},
      repositories: {},
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.almInstances.length === 0 && this.props.almInstances.length > 0) {
      this.setState({ selectedAlmInstance: this.props.almInstances[0] }, () => {
        this.fetchData().catch(() => {
          /* noop */
        });
      });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchData = async () => {
    const { showPersonalAccessTokenForm } = this.state;

    if (!showPersonalAccessTokenForm) {
      this.setState({ loading: true });
      let projects: AzureProject[] | undefined;
      try {
        projects = await this.fetchAzureProjects();
      } catch (_) {
        if (this.mounted) {
          this.setState({ showPersonalAccessTokenForm: true, loading: false });
        }
      }

      const { repositories } = this.state;

      let firstProjectName: string;

      if (projects && projects.length > 0) {
        firstProjectName = projects[0].name;

        this.setState(({ loadingRepositories }) => ({
          loadingRepositories: { ...loadingRepositories, [firstProjectName]: true },
        }));

        const repos = await this.fetchAzureRepositories(firstProjectName);
        repositories[firstProjectName] = repos;
      }

      if (this.mounted) {
        this.setState(({ loadingRepositories }) => {
          if (firstProjectName !== '') {
            loadingRepositories[firstProjectName] = false;
          }

          return {
            loading: false,
            loadingRepositories: { ...loadingRepositories },
            projects,
            repositories,
          };
        });
      }
    }
  };

  fetchAzureProjects = (): Promise<AzureProject[] | undefined> => {
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance) {
      return Promise.resolve(undefined);
    }

    return getAzureProjects(selectedAlmInstance.key).then(({ projects }) => projects);
  };

  fetchAzureRepositories = (projectName: string): Promise<AzureRepository[]> => {
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance) {
      return Promise.resolve([]);
    }

    return getAzureRepositories(selectedAlmInstance.key, projectName)
      .then(({ repositories }) => repositories)
      .catch(() => []);
  };

  cleanUrl = () => {
    const { location, router } = this.props;
    delete location.query.resetPat;
    router.replace(location);
  };

  handleOpenProject = async (projectName: string) => {
    if (this.state.searchResults) {
      return;
    }

    this.setState(({ loadingRepositories }) => ({
      loadingRepositories: { ...loadingRepositories, [projectName]: true },
    }));

    const projectRepos = await this.fetchAzureRepositories(projectName);

    this.setState(({ loadingRepositories, repositories }) => ({
      loadingRepositories: { ...loadingRepositories, [projectName]: false },
      repositories: { ...repositories, [projectName]: projectRepos },
    }));
  };

  handleSearchRepositories = async (searchQuery: string) => {
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance) {
      return;
    }

    if (searchQuery.length === 0) {
      this.setState({ searchResults: undefined, searchQuery: undefined });
      return;
    }

    this.setState({ searching: true });

    const searchResults: AzureRepository[] = await searchAzureRepositories(
      selectedAlmInstance.key,
      searchQuery,
    )
      .then(({ repositories }) => repositories)
      .catch(() => []);

    if (this.mounted) {
      this.setState({
        searching: false,
        searchResults,
        searchQuery,
      });
    }
  };

  handleImportRepository = (selectedRepository: AzureRepository) => {
    const { selectedAlmInstance } = this.state;

    if (selectedAlmInstance && selectedRepository) {
      this.props.onProjectSetupDone({
        creationMode: CreateProjectModes.AzureDevOps,
        almSetting: selectedAlmInstance.key,
        projects: [
          {
            projectName: selectedRepository.projectName,
            repositoryName: selectedRepository.name,
          },
        ],
      });
    }
  };

  handlePersonalAccessTokenCreate = async () => {
    this.setState({ showPersonalAccessTokenForm: false });
    this.cleanUrl();
    await this.fetchData();
  };

  onSelectedAlmInstanceChange = (instance: AlmSettingsInstance) => {
    this.setState(
      {
        selectedAlmInstance: instance,
        searchResults: undefined,
        searchQuery: '',
        showPersonalAccessTokenForm: true,
      },
      () => {
        this.fetchData().catch(() => {
          /* noop */
        });
      },
    );
  };

  render() {
    const { canAdmin, loadingBindings, location, almInstances } = this.props;
    const {
      loading,
      loadingRepositories,
      showPersonalAccessTokenForm,
      projects,
      repositories,
      searching,
      searchResults,
      searchQuery,
      selectedAlmInstance,
    } = this.state;

    return (
      <AzureCreateProjectRenderer
        canAdmin={canAdmin}
        loading={loading || loadingBindings}
        loadingRepositories={loadingRepositories}
        onImportRepository={this.handleImportRepository}
        onOpenProject={this.handleOpenProject}
        onPersonalAccessTokenCreate={this.handlePersonalAccessTokenCreate}
        onSearch={this.handleSearchRepositories}
        projects={projects}
        repositories={repositories}
        searching={searching}
        searchResults={searchResults}
        searchQuery={searchQuery}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        resetPat={Boolean(location.query.resetPat)}
        showPersonalAccessTokenForm={
          showPersonalAccessTokenForm || Boolean(location.query.resetPat)
        }
        onSelectedAlmInstanceChange={this.onSelectedAlmInstanceChange}
      />
    );
  }
}
