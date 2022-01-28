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
import * as React from 'react';
import { WithRouterProps } from 'react-router';
import {
  importBitbucketCloudRepository,
  searchForBitbucketCloudRepositories
} from '../../../api/alm-integrations';
import { BitbucketCloudRepository } from '../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../types/alm-settings';
import { Paging } from '../../../types/types';
import BitbucketCloudProjectCreateRenderer from './BitbucketCloudProjectCreateRender';

interface Props extends Pick<WithRouterProps, 'location' | 'router'> {
  canAdmin: boolean;
  settings: AlmSettingsInstance[];
  loadingBindings: boolean;
  onProjectCreate: (projectKey: string) => void;
}

interface State {
  importingSlug?: string;
  isLastPage?: boolean;
  loading: boolean;
  loadingMore: boolean;
  projectsPaging: Omit<Paging, 'total'>;
  resetPat: boolean;
  repositories: BitbucketCloudRepository[];
  searching: boolean;
  searchQuery: string;
  settings: AlmSettingsInstance;
  showPersonalAccessTokenForm: boolean;
}

export const BITBUCKET_CLOUD_PROJECTS_PAGESIZE = 30;
export default class BitbucketCloudProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      // For now, we only handle a single instance. So we always use the first
      // one from the list.
      loading: false,
      loadingMore: false,
      resetPat: false,
      projectsPaging: { pageIndex: 1, pageSize: BITBUCKET_CLOUD_PROJECTS_PAGESIZE },
      repositories: [],
      searching: false,
      searchQuery: '',
      settings: props.settings[0],
      showPersonalAccessTokenForm: true
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.settings.length === 0 && this.props.settings.length > 0) {
      this.setState({ settings: this.props.settings[0] }, () => this.fetchData());
    }
  }

  handlePersonalAccessTokenCreated = async () => {
    this.setState({ showPersonalAccessTokenForm: false });
    this.cleanUrl();
    this.setState({ loading: true });
    await this.fetchData();
    this.setState({ loading: false });
  };

  cleanUrl = () => {
    const { location, router } = this.props;
    delete location.query.resetPat;
    router.replace(location);
  };

  async fetchData(more = false) {
    const {
      settings,
      searchQuery,
      projectsPaging: { pageIndex, pageSize },
      showPersonalAccessTokenForm
    } = this.state;
    if (settings && !showPersonalAccessTokenForm) {
      const { isLastPage, repositories } = await searchForBitbucketCloudRepositories(
        settings.key,
        searchQuery,
        pageSize,
        pageIndex
      ).catch(() => {
        this.handleError();
        return { isLastPage: undefined, repositories: undefined };
      });
      if (this.mounted && isLastPage !== undefined && repositories !== undefined) {
        if (more) {
          this.setState(state => ({
            isLastPage,
            repositories: [...state.repositories, ...repositories]
          }));
        } else {
          this.setState({ isLastPage, repositories });
        }
      }
    }
  }

  handleError = () => {
    if (this.mounted) {
      this.setState({
        projectsPaging: { pageIndex: 1, pageSize: BITBUCKET_CLOUD_PROJECTS_PAGESIZE },
        repositories: [],
        resetPat: true,
        showPersonalAccessTokenForm: true
      });
    }

    return undefined;
  };

  handleSearch = (searchQuery: string) => {
    this.setState(
      {
        searching: true,
        projectsPaging: { pageIndex: 1, pageSize: BITBUCKET_CLOUD_PROJECTS_PAGESIZE },
        searchQuery
      },
      async () => {
        await this.fetchData();
        if (this.mounted) {
          this.setState({ searching: false });
        }
      }
    );
  };

  handleLoadMore = () => {
    this.setState(
      state => ({
        loadingMore: true,
        projectsPaging: {
          pageIndex: state.projectsPaging.pageIndex + 1,
          pageSize: state.projectsPaging.pageSize
        }
      }),
      async () => {
        await this.fetchData(true);
        if (this.mounted) {
          this.setState({ loadingMore: false });
        }
      }
    );
  };

  handleImport = async (repositorySlug: string) => {
    const { settings } = this.state;

    if (!settings) {
      return;
    }

    this.setState({ importingSlug: repositorySlug });

    const result = await importBitbucketCloudRepository(settings.key, repositorySlug).catch(
      () => undefined
    );

    if (this.mounted) {
      this.setState({ importingSlug: undefined });

      if (result) {
        this.props.onProjectCreate(result.project.key);
      }
    }
  };

  render() {
    const { canAdmin, loadingBindings, location } = this.props;
    const {
      importingSlug,
      isLastPage = true,
      settings,
      loading,
      loadingMore,
      repositories,
      showPersonalAccessTokenForm,
      resetPat,
      searching,
      searchQuery
    } = this.state;
    return (
      <BitbucketCloudProjectCreateRenderer
        importingSlug={importingSlug}
        isLastPage={isLastPage}
        settings={settings}
        canAdmin={canAdmin}
        loadingMore={loadingMore}
        loading={loading || loadingBindings}
        onImport={this.handleImport}
        onLoadMore={this.handleLoadMore}
        onPersonalAccessTokenCreated={this.handlePersonalAccessTokenCreated}
        onSearch={this.handleSearch}
        repositories={repositories}
        searching={searching}
        searchQuery={searchQuery}
        resetPat={resetPat || Boolean(location.query.resetPat)}
        showPersonalAccessTokenForm={
          showPersonalAccessTokenForm || Boolean(location.query.resetPat)
        }
      />
    );
  }
}
