/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { getHostUrl } from 'sonar-ui-common/helpers/urls';
import {
  getGithubClientId,
  getGithubOrganizations,
  getGithubRepositories
} from '../../../api/alm-integrations';
import { GithubOrganization, GithubRepository } from '../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import GitHubProjectCreateRenderer from './GitHubProjectCreateRenderer';

interface Props {
  canAdmin: boolean;
  code?: string;
  settings?: AlmSettingsInstance;
}

interface State {
  error: boolean;
  loading: boolean;
  loadingRepositories: boolean;
  organizations: GithubOrganization[];
  repositoryPaging: T.Paging;
  repositories: GithubRepository[];
  searchQuery: string;
  selectedOrganization?: GithubOrganization;
  selectedRepository?: GithubRepository;
}

const REPOSITORY_PAGE_SIZE = 30;

export default class GitHubProjectCreate extends React.Component<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      error: false,
      loading: true,
      loadingRepositories: false,
      organizations: [],
      repositories: [],
      repositoryPaging: { pageSize: REPOSITORY_PAGE_SIZE, total: 0, pageIndex: 1 },
      searchQuery: ''
    };

    this.triggerSearch = debounce(this.triggerSearch, 250);
  }

  componentDidMount() {
    this.mounted = true;

    this.initialize();
  }

  componentDidUpdate(prevProps: Props) {
    if (!prevProps.settings && this.props.settings) {
      this.initialize();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  async initialize() {
    const { code, settings } = this.props;

    if (!settings) {
      this.setState({ error: true });
      return;
    } else {
      this.setState({ error: false });
    }

    try {
      if (!code) {
        await this.redirectToGithub(settings);
      } else {
        await this.fetchOrganizations(settings, code);
      }
    } catch (e) {
      if (this.mounted) {
        this.setState({ error: true });
      }
    }
  }

  async redirectToGithub(settings: AlmSettingsInstance) {
    const { clientId } = await getGithubClientId(settings.key);

    const queryParams = [
      { param: 'client_id', value: clientId },
      { param: 'redirect_uri', value: `${getHostUrl()}/projects/create?mode=${AlmKeys.GitHub}` }
    ]
      .map(({ param, value }) => `${param}=${value}`)
      .join('&');

    window.location.replace(`https://github.com/login/oauth/authorize?${queryParams}`);
  }

  async fetchOrganizations(settings: AlmSettingsInstance, token: string) {
    const { organizations } = await getGithubOrganizations(settings.key, token);

    if (this.mounted) {
      this.setState({ loading: false, organizations });
    }
  }

  async fetchRepositories(params: { organizationKey: string; page?: number; query?: string }) {
    const { organizationKey, page = 1, query } = params;
    const { settings } = this.props;

    if (!settings) {
      this.setState({ error: true });
      return;
    }

    this.setState({ loadingRepositories: true });

    const data = await getGithubRepositories(settings.key, organizationKey, page, query);

    if (this.mounted) {
      this.setState(({ repositories }) => ({
        loadingRepositories: false,
        repositoryPaging: data.paging,
        repositories: page === 1 ? data.repositories : [...repositories, ...data.repositories]
      }));
    }
  }

  triggerSearch = (query: string) => {
    const { selectedOrganization } = this.state;
    if (selectedOrganization) {
      this.fetchRepositories({ organizationKey: selectedOrganization.key, query });
    }
  };

  handleSelectOrganization = (key: string) => {
    this.setState(({ organizations }) => ({
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

  render() {
    const { canAdmin } = this.props;
    const {
      error,
      loading,
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
        loading={loading}
        loadingRepositories={loadingRepositories}
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
