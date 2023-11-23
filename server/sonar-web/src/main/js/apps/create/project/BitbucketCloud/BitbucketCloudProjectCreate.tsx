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
import { searchForBitbucketCloudRepositories } from '../../../../api/alm-integrations';
import { Location, Router } from '../../../../components/hoc/withRouter';
import { BitbucketCloudRepository } from '../../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../../types/alm-settings';
import { Paging } from '../../../../types/types';
import { ImportProjectParam } from '../CreateProjectPage';
import { BITBUCKET_CLOUD_PROJECTS_PAGESIZE } from '../constants';
import { CreateProjectModes } from '../types';
import BitbucketCloudProjectCreateRenderer from './BitbucketCloudProjectCreateRender';

interface Props {
  canAdmin: boolean;
  almInstances: AlmSettingsInstance[];
  loadingBindings: boolean;
  location: Location;
  router: Router;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
}

interface State {
  isLastPage?: boolean;
  loading: boolean;
  loadingMore: boolean;
  projectsPaging: Omit<Paging, 'total'>;
  resetPat: boolean;
  repositories: BitbucketCloudRepository[];
  searching: boolean;
  searchQuery: string;
  selectedAlmInstance: AlmSettingsInstance;
  showPersonalAccessTokenForm: boolean;
}

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
      selectedAlmInstance: props.almInstances[0],
      showPersonalAccessTokenForm: true,
    };
  }

  componentDidMount() {
    this.mounted = true;
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

  handlePersonalAccessTokenCreated = () => {
    this.cleanUrl();

    this.setState({ loading: true, showPersonalAccessTokenForm: false }, () => {
      this.fetchData()
        .then(() => this.setState({ loading: false }))
        .catch(() => {
          /* noop */
        });
    });
  };

  cleanUrl = () => {
    const { location, router } = this.props;
    delete location.query.resetPat;
    router.replace(location);
  };

  async fetchData(more = false) {
    const {
      selectedAlmInstance,
      searchQuery,
      projectsPaging: { pageIndex, pageSize },
      showPersonalAccessTokenForm,
    } = this.state;
    if (selectedAlmInstance && !showPersonalAccessTokenForm) {
      const { isLastPage, repositories } = await searchForBitbucketCloudRepositories(
        selectedAlmInstance.key,
        searchQuery,
        pageSize,
        pageIndex,
      ).catch(() => {
        this.handleError();
        return { isLastPage: undefined, repositories: undefined };
      });
      if (this.mounted && isLastPage !== undefined && repositories !== undefined) {
        if (more) {
          this.setState((state) => ({
            isLastPage,
            repositories: [...state.repositories, ...repositories],
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
        showPersonalAccessTokenForm: true,
      });
    }

    return undefined;
  };

  handleSearch = (searchQuery: string) => {
    this.setState(
      {
        searching: true,
        projectsPaging: { pageIndex: 1, pageSize: BITBUCKET_CLOUD_PROJECTS_PAGESIZE },
        searchQuery,
      },
      () => {
        this.fetchData().then(
          () => {
            if (this.mounted) {
              this.setState({ searching: false });
            }
          },
          () => {
            /* noop */
          },
        );
      },
    );
  };

  handleLoadMore = () => {
    this.setState(
      (state) => ({
        loadingMore: true,
        projectsPaging: {
          pageIndex: state.projectsPaging.pageIndex + 1,
          pageSize: state.projectsPaging.pageSize,
        },
      }),
      () => {
        this.fetchData(true).then(
          () => {
            if (this.mounted) {
              this.setState({ loadingMore: false });
            }
          },
          () => {
            /* noop */
          },
        );
      },
    );
  };

  handleImport = (repositorySlug: string) => {
    const { selectedAlmInstance } = this.state;

    if (selectedAlmInstance) {
      this.props.onProjectSetupDone({
        creationMode: CreateProjectModes.BitbucketCloud,
        almSetting: selectedAlmInstance.key,
        projects: [
          {
            repositorySlug,
          },
        ],
      });
    }
  };

  onSelectedAlmInstanceChange = (instance: AlmSettingsInstance) => {
    this.setState({
      selectedAlmInstance: instance,
      showPersonalAccessTokenForm: true,
      resetPat: false,
      searching: false,
      searchQuery: '',
      projectsPaging: { pageIndex: 1, pageSize: BITBUCKET_CLOUD_PROJECTS_PAGESIZE },
    });
  };

  render() {
    const { canAdmin, loadingBindings, location, almInstances } = this.props;
    const {
      isLastPage = true,
      selectedAlmInstance,
      loading,
      loadingMore,
      repositories,
      showPersonalAccessTokenForm,
      resetPat,
      searching,
      searchQuery,
    } = this.state;
    return (
      <BitbucketCloudProjectCreateRenderer
        isLastPage={isLastPage}
        selectedAlmInstance={selectedAlmInstance}
        almInstances={almInstances}
        canAdmin={canAdmin}
        loadingMore={loadingMore}
        loading={loading || loadingBindings}
        onImport={this.handleImport}
        onLoadMore={this.handleLoadMore}
        onPersonalAccessTokenCreated={this.handlePersonalAccessTokenCreated}
        onSearch={this.handleSearch}
        onSelectedAlmInstanceChange={this.onSelectedAlmInstanceChange}
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
