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
import { debounce } from 'lodash';
import * as React from 'react';
import { isWebUri } from 'valid-url';
import {
  getGithubClientId,
  getGithubOrganizations,
  getGithubRepositories,
} from '../../../../api/alm-integrations';
import { Location, Router } from '../../../../components/hoc/withRouter';
import { getHostUrl } from '../../../../helpers/urls';
import { GithubOrganization, GithubRepository } from '../../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';
import { Paging } from '../../../../types/types';
import { ImportProjectParam } from '../CreateProjectPage';
import { CreateProjectModes } from '../types';
import GitHubProjectCreateRenderer from './GitHubProjectCreateRenderer';

interface Props {
  canAdmin: boolean;
  loadingBindings: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
  almInstances: AlmSettingsInstance[];
  location: Location;
  router: Router;
}

interface State {
  error: boolean;
  loadingOrganizations: boolean;
  loadingRepositories: boolean;
  organizations: GithubOrganization[];
  repositoryPaging: Paging;
  repositories: GithubRepository[];
  searchQuery: string;
  selectedOrganization?: GithubOrganization;
  selectedAlmInstance?: AlmSettingsInstance;
}

const REPOSITORY_PAGE_SIZE = 50;

export default class GitHubProjectCreate extends React.Component<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      error: false,
      loadingOrganizations: true,
      loadingRepositories: false,
      organizations: [],
      repositories: [],
      repositoryPaging: { pageSize: REPOSITORY_PAGE_SIZE, total: 0, pageIndex: 1 },
      searchQuery: '',
      selectedAlmInstance: this.getInitialSelectedAlmInstance(),
    };

    this.triggerSearch = debounce(this.triggerSearch, 250);
  }

  componentDidMount() {
    this.mounted = true;
    this.initialize();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.almInstances.length === 0 && this.props.almInstances.length > 0) {
      this.setState({ selectedAlmInstance: this.getInitialSelectedAlmInstance() }, () => {
        this.initialize().catch(() => {
          /* noop */
        });
      });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getInitialSelectedAlmInstance() {
    const {
      location: {
        query: { almInstance: selectedAlmInstanceKey },
      },
      almInstances,
    } = this.props;
    const selectedAlmInstance = almInstances.find(
      (instance) => instance.key === selectedAlmInstanceKey,
    );
    if (selectedAlmInstance) {
      return selectedAlmInstance;
    }
    return this.props.almInstances.length > 1 ? undefined : this.props.almInstances[0];
  }

  async initialize() {
    const { location, router } = this.props;
    const { selectedAlmInstance } = this.state;
    if (!selectedAlmInstance || !selectedAlmInstance.url) {
      this.setState({ error: true });
      return;
    }
    this.setState({ error: false });

    const code = location.query?.code;
    try {
      if (!code) {
        await this.redirectToGithub(selectedAlmInstance);
      } else {
        delete location.query.code;
        router.replace(location);
        await this.fetchOrganizations(selectedAlmInstance, code);
      }
    } catch (e) {
      if (this.mounted) {
        this.setState({ error: true });
      }
    }
  }

  async redirectToGithub(selectedAlmInstance: AlmSettingsInstance) {
    if (!selectedAlmInstance.url) {
      return;
    }

    const { clientId } = await getGithubClientId(selectedAlmInstance.key);

    if (!clientId) {
      this.setState({ error: true });
      return;
    }
    const queryParams = [
      { param: 'client_id', value: clientId },
      {
        param: 'redirect_uri',
        value: encodeURIComponent(
          `${getHostUrl()}/projects/create?mode=${AlmKeys.GitHub}&almInstance=${
            selectedAlmInstance.key
          }`,
        ),
      },
    ]
      .map(({ param, value }) => `${param}=${value}`)
      .join('&');

    let instanceRootUrl;
    // Strip the api section from the url, since we're not hitting the api here.
    if (selectedAlmInstance.url.includes('/api/v3')) {
      // GitHub Enterprise
      instanceRootUrl = selectedAlmInstance.url.replace('/api/v3', '');
    } else {
      // github.com
      instanceRootUrl = selectedAlmInstance.url.replace('api.', '');
    }

    // strip the trailing /
    instanceRootUrl = instanceRootUrl.replace(/\/$/, '');
    if (!isWebUri(instanceRootUrl)) {
      this.setState({ error: true });
    } else {
      window.location.replace(`${instanceRootUrl}/login/oauth/authorize?${queryParams}`);
    }
  }

  async fetchOrganizations(selectedAlmInstance: AlmSettingsInstance, token: string) {
    const { organizations } = await getGithubOrganizations(selectedAlmInstance.key, token);

    if (this.mounted) {
      this.setState({ loadingOrganizations: false, organizations });
    }
  }

  async fetchRepositories(params: { organizationKey: string; page?: number; query?: string }) {
    const { organizationKey, page = 1, query } = params;
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance) {
      this.setState({ error: true });
      return;
    }

    this.setState({ loadingRepositories: true });

    try {
      const data = await getGithubRepositories({
        almSetting: selectedAlmInstance.key,
        organization: organizationKey,
        pageSize: REPOSITORY_PAGE_SIZE,
        page,
        query,
      });

      if (this.mounted) {
        this.setState(({ repositories }) => ({
          loadingRepositories: false,
          repositoryPaging: data.paging,
          repositories: page === 1 ? data.repositories : [...repositories, ...data.repositories],
        }));
      }
    } catch (_) {
      if (this.mounted) {
        this.setState({
          loadingRepositories: false,
          repositoryPaging: { pageIndex: 1, pageSize: REPOSITORY_PAGE_SIZE, total: 0 },
          repositories: [],
        });
      }
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
      searchQuery: '',
      selectedOrganization: organizations.find((o) => o.key === key),
    }));
    this.fetchRepositories({ organizationKey: key });
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
        query: searchQuery,
      });
    }
  };

  handleImportRepository = (repoKeys: string[]) => {
    const { selectedOrganization, selectedAlmInstance } = this.state;

    if (selectedAlmInstance && selectedOrganization && repoKeys.length > 0) {
      this.props.onProjectSetupDone({
        almSetting: selectedAlmInstance.key,
        creationMode: CreateProjectModes.GitHub,
        projects: repoKeys.map((repositoryKey) => ({ repositoryKey })),
      });
    }
  };

  onSelectedAlmInstanceChange = (instance: AlmSettingsInstance) => {
    this.setState(
      { selectedAlmInstance: instance, searchQuery: '', organizations: [], repositories: [] },
      () => {
        this.initialize().catch(() => {
          /* noop */
        });
      },
    );
  };

  render() {
    const { canAdmin, loadingBindings, almInstances } = this.props;
    const {
      error,
      loadingOrganizations,
      loadingRepositories,
      organizations,
      repositoryPaging,
      repositories,
      searchQuery,
      selectedOrganization,
      selectedAlmInstance,
    } = this.state;

    return (
      <GitHubProjectCreateRenderer
        canAdmin={canAdmin}
        error={error}
        loadingBindings={loadingBindings}
        loadingOrganizations={loadingOrganizations}
        loadingRepositories={loadingRepositories}
        onImportRepository={this.handleImportRepository}
        onLoadMore={this.handleLoadMore}
        onSearch={this.handleSearch}
        onSelectOrganization={this.handleSelectOrganization}
        organizations={organizations}
        repositoryPaging={repositoryPaging}
        searchQuery={searchQuery}
        repositories={repositories}
        selectedOrganization={selectedOrganization}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        onSelectedAlmInstanceChange={this.onSelectedAlmInstanceChange}
      />
    );
  }
}
