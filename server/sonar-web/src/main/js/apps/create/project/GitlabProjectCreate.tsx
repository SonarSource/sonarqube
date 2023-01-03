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
import { WithRouterProps } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  checkPersonalAccessTokenIsValid,
  getGitlabProjects,
  importGitlabProject,
  setAlmPersonalAccessToken
} from '../../../api/alm-integrations';
import { GitlabProject } from '../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import GitlabProjectCreateRenderer from './GitlabProjectCreateRenderer';

interface Props extends Pick<WithRouterProps, 'location' | 'router'> {
  canAdmin: boolean;
  loadingBindings: boolean;
  onProjectCreate: (projectKeys: string[]) => void;
  settings: AlmSettingsInstance[];
}

interface State {
  importingGitlabProjectId?: string;
  loading: boolean;
  loadingMore: boolean;
  projects?: GitlabProject[];
  projectsPaging: T.Paging;
  submittingToken: boolean;
  tokenIsValid: boolean;
  tokenValidationErrorMessage?: string;
  searching: boolean;
  searchQuery: string;
  settings?: AlmSettingsInstance;
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
      tokenIsValid: false,
      searching: false,
      searchQuery: '',
      settings: props.settings.length === 1 ? props.settings[0] : undefined,
      submittingToken: false
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

    const { status, error } = await this.checkPersonalAccessToken();

    let result;
    if (status) {
      result = await this.fetchProjects();
    }

    if (this.mounted) {
      if (result) {
        const { projects, projectsPaging } = result;

        this.setState({
          tokenIsValid: status,
          loading: false,
          projects,
          projectsPaging
        });
      } else {
        this.setState({
          loading: false,
          tokenValidationErrorMessage: !status ? error : undefined
        });
      }
    }
  };

  checkPersonalAccessToken = () => {
    const { settings } = this.state;

    if (!settings) {
      return Promise.resolve({
        status: false,
        error: translate('onboarding.create_project.pat_incorrect', AlmKeys.GitLab)
      });
    }

    return checkPersonalAccessTokenIsValid(settings.key);
  };

  handleError = () => {
    if (this.mounted) {
      this.setState({ tokenIsValid: false });
    }

    return undefined;
  };

  fetchProjects = async (pageIndex = 1, query?: string) => {
    const { settings } = this.state;

    if (!settings) {
      return Promise.resolve(undefined);
    }

    try {
      return await getGitlabProjects({
        almSetting: settings.key,
        page: pageIndex,
        pageSize: GITLAB_PROJECTS_PAGESIZE,
        query
      });
    } catch (_) {
      return this.handleError();
    }
  };

  doImport = async (gitlabProjectId: string) => {
    const { settings } = this.state;

    if (!settings) {
      return Promise.resolve(undefined);
    }

    try {
      return await importGitlabProject({
        almSetting: settings.key,
        gitlabProjectId
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
        this.props.onProjectCreate([result.project.key]);
      }
    }
  };

  handleLoadMore = async () => {
    this.setState({ loadingMore: true });

    const {
      projectsPaging: { pageIndex },
      searchQuery
    } = this.state;

    const result = await this.fetchProjects(pageIndex + 1, searchQuery);

    if (this.mounted) {
      this.setState(({ projects = [], projectsPaging }) => ({
        loadingMore: false,
        projects: result ? [...projects, ...result.projects] : projects,
        projectsPaging: result ? result.projectsPaging : projectsPaging
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
        projectsPaging: result ? result.projectsPaging : projectsPaging
      }));
    }
  };

  cleanUrl = () => {
    const { location, router } = this.props;
    delete location.query.resetPat;
    router.replace(location);
  };

  handlePersonalAccessTokenCreate = async (token: string) => {
    const { settings } = this.state;

    if (!settings || token.length < 1) {
      return;
    }

    this.setState({ submittingToken: true, tokenValidationErrorMessage: undefined });

    try {
      await setAlmPersonalAccessToken(settings.key, token);

      const { status, error } = await this.checkPersonalAccessToken();

      if (this.mounted) {
        this.setState({
          submittingToken: false,
          tokenIsValid: status,
          tokenValidationErrorMessage: error
        });

        if (status) {
          this.cleanUrl();
          await this.fetchInitialData();
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
      importingGitlabProjectId,
      loading,
      loadingMore,
      projects,
      projectsPaging,
      tokenIsValid,
      searching,
      searchQuery,
      settings,
      submittingToken,
      tokenValidationErrorMessage
    } = this.state;

    return (
      <GitlabProjectCreateRenderer
        settings={settings}
        canAdmin={canAdmin}
        importingGitlabProjectId={importingGitlabProjectId}
        loading={loading || loadingBindings}
        loadingMore={loadingMore}
        onImport={this.handleImport}
        onLoadMore={this.handleLoadMore}
        onPersonalAccessTokenCreate={this.handlePersonalAccessTokenCreate}
        onSearch={this.handleSearch}
        projects={projects}
        projectsPaging={projectsPaging}
        searching={searching}
        searchQuery={searchQuery}
        showPersonalAccessTokenForm={!tokenIsValid || Boolean(location.query.resetPat)}
        submittingToken={submittingToken}
        tokenValidationErrorMessage={tokenValidationErrorMessage}
      />
    );
  }
}
