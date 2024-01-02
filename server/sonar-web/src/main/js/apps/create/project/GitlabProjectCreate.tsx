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
import { getGitlabProjects, importGitlabProject } from '../../../api/alm-integrations';
import { Location, Router } from '../../../components/hoc/withRouter';
import { GitlabProject } from '../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../types/alm-settings';
import { Paging } from '../../../types/types';
import GitlabProjectCreateRenderer from './GitlabProjectCreateRenderer';

interface Props {
  canAdmin: boolean;
  loadingBindings: boolean;
  onProjectCreate: (projectKey: string) => void;
  almInstances: AlmSettingsInstance[];
  location: Location;
  router: Router;
}

interface State {
  importingGitlabProjectId?: string;
  loading: boolean;
  loadingMore: boolean;
  projects?: GitlabProject[];
  projectsPaging: Paging;
  resetPat: boolean;
  searching: boolean;
  searchQuery: string;
  selectedAlmInstance: AlmSettingsInstance;
  showPersonalAccessTokenForm: boolean;
}

const GITLAB_PROJECTS_PAGESIZE = 30;

export default class GitlabProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: false,
      loadingMore: false,
      projectsPaging: { pageIndex: 1, total: 0, pageSize: GITLAB_PROJECTS_PAGESIZE },
      resetPat: false,
      showPersonalAccessTokenForm: true,
      searching: false,
      searchQuery: '',
      selectedAlmInstance: props.almInstances[0],
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentDidUpdate(prevProps: Props) {
    const { almInstances } = this.props;
    if (prevProps.almInstances.length === 0 && this.props.almInstances.length > 0) {
      this.setState({ selectedAlmInstance: almInstances[0] }, () => this.fetchInitialData());
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchInitialData = async () => {
    const { showPersonalAccessTokenForm } = this.state;

    if (!showPersonalAccessTokenForm) {
      this.setState({ loading: true });
      const result = await this.fetchProjects();
      if (this.mounted && result) {
        const { projects, projectsPaging } = result;

        this.setState({
          loading: false,
          projects,
          projectsPaging,
        });
      } else {
        this.setState({
          loading: false,
        });
      }
    }
  };

  handleError = () => {
    if (this.mounted) {
      this.setState({ resetPat: true, showPersonalAccessTokenForm: true });
    }

    return undefined;
  };

  fetchProjects = async (pageIndex = 1, query?: string) => {
    const { selectedAlmInstance } = this.state;
    if (!selectedAlmInstance) {
      return Promise.resolve(undefined);
    }

    try {
      return await getGitlabProjects({
        almSetting: selectedAlmInstance.key,
        page: pageIndex,
        pageSize: GITLAB_PROJECTS_PAGESIZE,
        query,
      });
    } catch (_) {
      return this.handleError();
    }
  };

  doImport = async (gitlabProjectId: string) => {
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance) {
      return Promise.resolve(undefined);
    }

    try {
      return await importGitlabProject({
        almSetting: selectedAlmInstance.key,
        gitlabProjectId,
      });
    } catch (_) {
      return this.handleError();
    }
  };

  handleImport = async (gitlabProjectId: string) => {
    this.setState({ importingGitlabProjectId: gitlabProjectId });

    const result = await this.doImport(gitlabProjectId);

    if (this.mounted) {
      this.setState({ importingGitlabProjectId: undefined });

      if (result) {
        this.props.onProjectCreate(result.project.key);
      }
    }
  };

  handleLoadMore = async () => {
    this.setState({ loadingMore: true });

    const {
      projectsPaging: { pageIndex },
      searchQuery,
    } = this.state;

    const result = await this.fetchProjects(pageIndex + 1, searchQuery);
    if (this.mounted) {
      this.setState(({ projects = [], projectsPaging }) => ({
        loadingMore: false,
        projects: result ? [...projects, ...result.projects] : projects,
        projectsPaging: result ? result.projectsPaging : projectsPaging,
      }));
    }
  };

  handleSearch = async (searchQuery: string) => {
    this.setState({ searching: true, searchQuery });

    const result = await this.fetchProjects(1, searchQuery);
    if (this.mounted) {
      this.setState(({ projects, projectsPaging }) => ({
        searching: false,
        projects: result ? result.projects : projects,
        projectsPaging: result ? result.projectsPaging : projectsPaging,
      }));
    }
  };

  cleanUrl = () => {
    const { location, router } = this.props;
    delete location.query.resetPat;
    router.replace(location);
  };

  handlePersonalAccessTokenCreated = async () => {
    this.setState({ showPersonalAccessTokenForm: false, resetPat: false });
    this.cleanUrl();
    await this.fetchInitialData();
  };

  onSelectedAlmInstanceChange = (instance: AlmSettingsInstance) => {
    this.setState({
      selectedAlmInstance: instance,
      showPersonalAccessTokenForm: true,
      projects: undefined,
      resetPat: false,
      searchQuery: '',
    });
  };

  render() {
    const { loadingBindings, location, almInstances, canAdmin } = this.props;
    const {
      importingGitlabProjectId,
      loading,
      loadingMore,
      projects,
      projectsPaging,
      resetPat,
      searching,
      searchQuery,
      selectedAlmInstance,
      showPersonalAccessTokenForm,
    } = this.state;

    return (
      <GitlabProjectCreateRenderer
        canAdmin={canAdmin}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        importingGitlabProjectId={importingGitlabProjectId}
        loading={loading || loadingBindings}
        loadingMore={loadingMore}
        onImport={this.handleImport}
        onLoadMore={this.handleLoadMore}
        onPersonalAccessTokenCreated={this.handlePersonalAccessTokenCreated}
        onSearch={this.handleSearch}
        projects={projects}
        projectsPaging={projectsPaging}
        resetPat={resetPat || Boolean(location.query.resetPat)}
        searching={searching}
        searchQuery={searchQuery}
        showPersonalAccessTokenForm={
          showPersonalAccessTokenForm || Boolean(location.query.resetPat)
        }
        onSelectedAlmInstanceChange={this.onSelectedAlmInstanceChange}
      />
    );
  }
}
