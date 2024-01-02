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
import * as React from 'react';
import {
  checkPersonalAccessTokenIsValid,
  getAzureProjects,
  getAzureRepositories,
  importAzureRepository,
  searchAzureRepositories,
  setAlmPersonalAccessToken,
} from '../../../api/alm-integrations';
import { Location, Router } from '../../../components/hoc/withRouter';
import { AzureProject, AzureRepository } from '../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../types/alm-settings';
import { Dict } from '../../../types/types';
import AzureCreateProjectRenderer from './AzureProjectCreateRenderer';
import { tokenExistedBefore } from './utils';

interface Props {
  canAdmin: boolean;
  loadingBindings: boolean;
  onProjectCreate: (projectKey: string) => void;
  almInstances: AlmSettingsInstance[];
  location: Location;
  router: Router;
}

interface State {
  importing: boolean;
  loading: boolean;
  loadingRepositories: Dict<boolean>;
  patIsValid?: boolean;
  projects?: AzureProject[];
  repositories: Dict<AzureRepository[]>;
  searching?: boolean;
  searchResults?: AzureRepository[];
  searchQuery?: string;
  selectedRepository?: AzureRepository;
  selectedAlmInstance?: AlmSettingsInstance;
  submittingToken?: boolean;
  tokenValidationFailed: boolean;
  firstConnection?: boolean;
}

export default class AzureProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      // For now, we only handle a single instance. So we always use the first
      // one from the list.
      selectedAlmInstance: props.almInstances[0],
      importing: false,
      loading: false,
      loadingRepositories: {},
      repositories: {},
      tokenValidationFailed: false,
      firstConnection: false,
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.almInstances.length === 0 && this.props.almInstances.length > 0) {
      this.setState({ selectedAlmInstance: this.props.almInstances[0] }, () => this.fetchData());
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchData = async () => {
    this.setState({ loading: true });

    const { patIsValid, error } = await this.checkPersonalAccessToken();

    let projects: AzureProject[] | undefined;
    if (patIsValid) {
      projects = await this.fetchAzureProjects();
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
        if (firstProjectName) {
          loadingRepositories[firstProjectName] = false;
        }

        return {
          patIsValid,
          loading: false,
          loadingRepositories: { ...loadingRepositories },
          projects,
          repositories,
          firstConnection: tokenExistedBefore(error),
        };
      });
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
      searchQuery
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

  handleImportRepository = async () => {
    const { selectedRepository, selectedAlmInstance } = this.state;

    if (!selectedAlmInstance || !selectedRepository) {
      return;
    }

    this.setState({ importing: true });

    const createdProject = await importAzureRepository(
      selectedAlmInstance.key,
      selectedRepository.projectName,
      selectedRepository.name
    )
      .then(({ project }) => project)
      .catch(() => undefined);

    if (this.mounted) {
      this.setState({ importing: false });
      if (createdProject) {
        this.props.onProjectCreate(createdProject.key);
      }
    }
  };

  handleSelectRepository = (selectedRepository: AzureRepository) => {
    this.setState({ selectedRepository });
  };

  checkPersonalAccessToken = () => {
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance) {
      return Promise.resolve({ patIsValid: false, error: '' });
    }

    return checkPersonalAccessTokenIsValid(selectedAlmInstance.key).then(({ status, error }) => {
      return { patIsValid: status, error };
    });
  };

  handlePersonalAccessTokenCreate = async (token: string) => {
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance || token.length < 1) {
      return;
    }

    this.setState({ submittingToken: true, tokenValidationFailed: false });

    try {
      await setAlmPersonalAccessToken(selectedAlmInstance.key, token);
      const { patIsValid } = await this.checkPersonalAccessToken();

      if (this.mounted) {
        this.setState({
          submittingToken: false,
          patIsValid,
          tokenValidationFailed: !patIsValid,
        });

        if (patIsValid) {
          this.cleanUrl();
          this.fetchData();
        }
      }
    } catch (e) {
      if (this.mounted) {
        this.setState({ submittingToken: false });
      }
    }
  };

  onSelectedAlmInstanceChange = (instance: AlmSettingsInstance) => {
    this.setState(
      { selectedAlmInstance: instance, searchResults: undefined, searchQuery: '' },
      () => this.fetchData()
    );
  };

  render() {
    const { canAdmin, loadingBindings, location, almInstances } = this.props;
    const {
      importing,
      loading,
      loadingRepositories,
      patIsValid,
      projects,
      repositories,
      searching,
      searchResults,
      searchQuery,
      selectedRepository,
      selectedAlmInstance,
      submittingToken,
      tokenValidationFailed,
      firstConnection,
    } = this.state;

    return (
      <AzureCreateProjectRenderer
        canAdmin={canAdmin}
        importing={importing}
        loading={loading || loadingBindings}
        loadingRepositories={loadingRepositories}
        onImportRepository={this.handleImportRepository}
        onOpenProject={this.handleOpenProject}
        onPersonalAccessTokenCreate={this.handlePersonalAccessTokenCreate}
        onSearch={this.handleSearchRepositories}
        onSelectRepository={this.handleSelectRepository}
        projects={projects}
        repositories={repositories}
        searching={searching}
        searchResults={searchResults}
        searchQuery={searchQuery}
        selectedRepository={selectedRepository}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        showPersonalAccessTokenForm={!patIsValid || Boolean(location.query.resetPat)}
        submittingToken={submittingToken}
        tokenValidationFailed={tokenValidationFailed}
        onSelectedAlmInstanceChange={this.onSelectedAlmInstanceChange}
        firstConnection={firstConnection}
      />
    );
  }
}
