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
import { debounce } from 'lodash';
import * as React from 'react';
import { WithRouterProps } from 'react-router';
import {
  getGithubClientId,
  getGithubOrganizations,
  getGithubRepositories,
  importGithubRepository
} from '../../../api/alm-integrations';
import { getHostUrl } from '../../../helpers/urls';
import { GithubOrganization, GithubRepository } from '../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import { Paging } from '../../../types/types';
import GitHubProjectCreateRenderer from './GitHubProjectCreateRenderer';

interface Props extends Pick<WithRouterProps, 'location' | 'router'> {
  canAdmin: boolean;
  loadingBindings: boolean;
  onProjectCreate: (projectKey: string) => void;
  settings: AlmSettingsInstance[];
}

interface State {
  error: boolean;
  importing: boolean;
  loadingOrganizations: boolean;
  loadingRepositories: boolean;
  organizations: GithubOrganization[];
  repositoryPaging: Paging;
  repositories: GithubRepository[];
  searchQuery: string;
  selectedOrganization?: GithubOrganization;
  selectedRepository?: GithubRepository;
  settings?: AlmSettingsInstance;
}

const REPOSITORY_PAGE_SIZE = 30;

export default class GitHubProjectCreate extends React.Component<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      error: false,
      importing: false,
      loadingOrganizations: true,
      loadingRepositories: false,
      organizations: [],
      repositories: [],
      repositoryPaging: { pageSize: REPOSITORY_PAGE_SIZE, total: 0, pageIndex: 1 },
      searchQuery: '',
      settings: props.settings[0]
    };

    this.triggerSearch = debounce(this.triggerSearch, 250);
  }

  componentDidMount() {
    this.mounted = true;

    this.initialize();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.settings.length === 0 && this.props.settings.length > 0) {
      this.setState({ settings: this.props.settings[0] }, () => this.initialize());
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  async initialize() {
    const { location, router } = this.props;
    const { settings } = this.state;

    if (!settings || !settings.url) {
      this.setState({ error: true });
      return;
    } else {
      this.setState({ error: false });
    }

    const code = location.query?.code;

    try {
      if (!code) {
        await this.redirectToGithub(settings);
      } else {
        delete location.query.code;
        router.replace(location);
        await this.fetchOrganizations(settings, code);
      }
    } catch (e) {
      if (this.mounted) {
        this.setState({ error: true });
      }
    }
  }

  async redirectToGithub(settings: AlmSettingsInstance) {
    if (!settings.url) {
      return;
    }

    const { clientId } = await getGithubClientId(settings.key);

    if (!clientId) {
      this.setState({ error: true });
      return;
    }

    const queryParams = [
      { param: 'client_id', value: clientId },
      { param: 'redirect_uri', value: `${getHostUrl()}/projects/create?mode=${AlmKeys.GitHub}` }
    ]
      .map(({ param, value }) => `${param}=${value}`)
      .join('&');

    let instanceRootUrl;
    // Strip the api section from the url, since we're not hitting the api here.
    if (settings.url.includes('/api/v3')) {
      // GitHub Enterprise
      instanceRootUrl = settings.url.replace('/api/v3', '');
    } else {
      // github.com
      instanceRootUrl = settings.url.replace('api.', '');
    }

    // strip the trailing /
    instanceRootUrl = instanceRootUrl.replace(/\/$/, '');
    window.location.replace(`${instanceRootUrl}/login/oauth/authorize?${queryParams}`);
  }

  async fetchOrganizations(settings: AlmSettingsInstance, token: string) {
    const { organizations } = await getGithubOrganizations(settings.key, token);

    if (this.mounted) {
      this.setState({ loadingOrganizations: false, organizations });
    }
  }

  async fetchRepositories(params: { organizationKey: string; page?: number; query?: string }) {
    const { organizationKey, page = 1, query } = params;
    const { settings } = this.state;

    if (!settings) {
      this.setState({ error: true });
      return;
    }

    this.setState({ loadingRepositories: true });

    try {
      const data = await getGithubRepositories({
        almSetting: settings.key,
        organization: organizationKey,
        pageSize: REPOSITORY_PAGE_SIZE,
        page,
        query
      });

      if (this.mounted) {
        this.setState(({ repositories }) => ({
          loadingRepositories: false,
          repositoryPaging: data.paging,
          repositories: page === 1 ? data.repositories : [...repositories, ...data.repositories]
        }));
      }
    } catch (_) {
      if (this.mounted) {
        this.setState({
          loadingRepositories: false,
          repositoryPaging: { pageIndex: 1, pageSize: REPOSITORY_PAGE_SIZE, total: 0 },
          repositories: []
        });
      }
    }
  }

  triggerSearch = (query: string) => {
    const { selectedOrganization } = this.state;
    if (selectedOrganization) {
      this.setState({ selectedRepository: undefined });
      this.fetchRepositories({ organizationKey: selectedOrganization.key, query });
    }
  };

  handleSelectOrganization = (key: string) => {
    this.setState(({ organizations }) => ({
      searchQuery: '',
      selectedRepository: undefined,
      selectedOrganization: organizations.find(o => o.key === key)
    }));
    this.fetchRepositories({ organizationKey: key });
  };

  handleSelectRepository = (key: string) => {
    this.setState(({ repositories }) => ({
      selectedRepository: repositories?.find(r => r.key === key)
    }));
  };

  handleSearch = (searchQuery: string) => {
    this.setState({ searchQuery });
    this.triggerSearch(searchQuery);
  };

  handleLoadMore = () => {
    const { repositoryPaging, searchQuery, selectedOrganization } = this.state;

    if (selectedOrganization) {
      this.fetchRepositories({
        organizationKey: selectedOrganization.key,
        page: repositoryPaging.pageIndex + 1,
        query: searchQuery
      });
    }
  };

  handleImportRepository = async () => {
    const { selectedOrganization, selectedRepository, settings } = this.state;

    if (settings && selectedOrganization && selectedRepository) {
      this.setState({ importing: true });

      try {
        const { project } = await importGithubRepository(
          settings.key,
          selectedOrganization.key,
          selectedRepository.key
        );

        this.props.onProjectCreate(project.key);
      } finally {
        if (this.mounted) {
          this.setState({ importing: false });
        }
      }
    }
  };

  render() {
    const { canAdmin, loadingBindings } = this.props;
    const {
      error,
      importing,
      loadingOrganizations,
      loadingRepositories,
      organizations,
      repositoryPaging,
      repositories,
      searchQuery,
      selectedOrganization,
      selectedRepository
    } = this.state;

    return (
      <GitHubProjectCreateRenderer
        canAdmin={canAdmin}
        error={error}
        importing={importing}
        loadingBindings={loadingBindings}
        loadingOrganizations={loadingOrganizations}
        loadingRepositories={loadingRepositories}
        onImportRepository={this.handleImportRepository}
        onLoadMore={this.handleLoadMore}
        onSearch={this.handleSearch}
        onSelectOrganization={this.handleSelectOrganization}
        onSelectRepository={this.handleSelectRepository}
        organizations={organizations}
        repositoryPaging={repositoryPaging}
        searchQuery={searchQuery}
        repositories={repositories}
        selectedOrganization={selectedOrganization}
        selectedRepository={selectedRepository}
      />
    );
  }
}
