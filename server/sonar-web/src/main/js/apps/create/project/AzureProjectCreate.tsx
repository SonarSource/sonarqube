/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { groupBy } from 'lodash';
import * as React from 'react';
import { WithRouterProps } from 'react-router';
import {
  checkPersonalAccessTokenIsValid,
  getAzureProjects,
  getAzureRepositories,
  importAzureRepository,
  searchAzureRepositories,
  setAlmPersonalAccessToken
} from '../../../api/alm-integrations';
import { AzureProject, AzureRepository } from '../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../types/alm-settings';
import { Dict } from '../../../types/types';
import AzureCreateProjectRenderer from './AzureProjectCreateRenderer';

interface Props extends Pick<WithRouterProps, 'location' | 'router'> {
  canAdmin: boolean;
  loadingBindings: boolean;
  onProjectCreate: (projectKey: string) => void;
  settings: AlmSettingsInstance[];
}

interface State {
  importing: boolean;
  loading: boolean;
  loadingRepositories: Dict<boolean>;
  patIsValid?: boolean;
  projects?: AzureProject[];
  repositories: Dict<AzureRepository[]>;
  searching?: boolean;
  searchResults?: Dict<AzureRepository[]>;
  searchQuery?: string;
  selectedRepository?: AzureRepository;
  settings?: AlmSettingsInstance;
  submittingToken?: boolean;
  tokenValidationFailed: boolean;
}

export default class AzureProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      // For now, we only handle a single instance. So we always use the first
      // one from the list.
      settings: props.settings[0],
      importing: false,
      loading: false,
      loadingRepositories: {},
      repositories: {},
      tokenValidationFailed: false
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchInitialData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.settings.length === 0 && this.props.settings.length > 0) {
      this.setState(
        { settings: this.props.settings.length === 1 ? this.props.settings[0] : undefined },
        () => this.fetchInitialData()
      );
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchInitialData = async () => {
    this.setState({ loading: true });

    const patIsValid = await this.checkPersonalAccessToken().catch(() => false);

    let projects: AzureProject[] | undefined;
    if (patIsValid) {
      projects = await this.fetchAzureProjects();
    }

    const { repositories } = this.state;

    let firstProjectName: string;

    if (projects && projects.length > 0) {
      firstProjectName = projects[0].name;

      this.setState(({ loadingRepositories }) => ({
        loadingRepositories: { ...loadingRepositories, [firstProjectName]: true }
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
          repositories
        };
      });
    }
  };

  fetchAzureProjects = (): Promise<AzureProject[] | undefined> => {
    const { settings } = this.state;

    if (!settings) {
      return Promise.resolve(undefined);
    }

    return getAzureProjects(settings.key).then(({ projects }) => projects);
  };

  fetchAzureRepositories = (projectName: string): Promise<AzureRepository[]> => {
    const { settings } = this.state;

    if (!settings) {
      return Promise.resolve([]);
    }

    return getAzureRepositories(settings.key, projectName)
      .then(({ repositories }) => repositories)
      .catch(() => []);
  };

  cleanUrl = () => {
    const { location, router } = this.props;
    delete location.query.resetPat;
    router.replace(location);
  };

  handleOpenProject = async (projectKey: string) => {
    if (this.state.searchResults) {
      return;
    }

    this.setState(({ loadingRepositories }) => ({
      loadingRepositories: { ...loadingRepositories, [projectKey]: true }
    }));

    const projectRepos = await this.fetchAzureRepositories(projectKey);

    this.setState(({ loadingRepositories, repositories }) => ({
      loadingRepositories: { ...loadingRepositories, [projectKey]: false },
      repositories: { ...repositories, [projectKey]: projectRepos }
    }));
  };

  handleSearchRepositories = async (searchQuery: string) => {
    const { settings } = this.state;

    if (!settings) {
      return;
    }

    if (searchQuery.length === 0) {
      this.setState({ searchResults: undefined, searchQuery: undefined });
      return;
    }

    this.setState({ searching: true });

    const results: AzureRepository[] = await searchAzureRepositories(settings.key, searchQuery)
      .then(({ repositories }) => repositories)
      .catch(() => []);

    if (this.mounted) {
      this.setState({
        searching: false,
        searchResults: groupBy(results, 'projectName'),
        searchQuery
      });
    }
  };

  handleImportRepository = async () => {
    const { selectedRepository, settings } = this.state;

    if (!settings || !selectedRepository) {
      return;
    }

    this.setState({ importing: true });

    const createdProject = await importAzureRepository(
      settings.key,
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
    const { settings } = this.state;

    if (!settings) {
      return Promise.resolve(false);
    }

    return checkPersonalAccessTokenIsValid(settings.key).then(({ status }) => status);
  };

  handlePersonalAccessTokenCreate = async (token: string) => {
    const { settings } = this.state;

    if (!settings || token.length < 1) {
      return;
    }

    this.setState({ submittingToken: true, tokenValidationFailed: false });

    try {
      await setAlmPersonalAccessToken(settings.key, token);
      const patIsValid = await this.checkPersonalAccessToken();

      if (this.mounted) {
        this.setState({ submittingToken: false, patIsValid, tokenValidationFailed: !patIsValid });

        if (patIsValid) {
          this.cleanUrl();
          this.fetchInitialData();
        }
      }
    } catch (e) {
      if (this.mounted) {
        this.setState({ submittingToken: false });
      }
    }
  };

  render() {
    const { canAdmin, loadingBindings, location } = this.props;
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
      settings,
      submittingToken,
      tokenValidationFailed
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
        settings={settings}
        showPersonalAccessTokenForm={!patIsValid || Boolean(location.query.resetPat)}
        submittingToken={submittingToken}
        tokenValidationFailed={tokenValidationFailed}
      />
    );
  }
}
