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
  getBitbucketServerProjects,
  getBitbucketServerRepositories,
  searchForBitbucketServerRepositories,
} from '../../../../api/alm-integrations';
import { Location, Router } from '../../../../components/hoc/withRouter';
import {
  BitbucketProject,
  BitbucketProjectRepositories,
  BitbucketRepository,
} from '../../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../../types/alm-settings';
import { ImportProjectParam } from '../CreateProjectPage';
import { DEFAULT_BBS_PAGE_SIZE } from '../constants';
import { CreateProjectModes } from '../types';
import BitbucketCreateProjectRenderer from './BitbucketProjectCreateRenderer';

interface Props {
  canAdmin: boolean;
  almInstances: AlmSettingsInstance[];
  loadingBindings: boolean;
  location: Location;
  router: Router;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
}

interface State {
  selectedAlmInstance?: AlmSettingsInstance;
  loading: boolean;
  projects?: BitbucketProject[];
  projectRepositories?: BitbucketProjectRepositories;
  searching: boolean;
  searchResults?: BitbucketRepository[];
  showPersonalAccessTokenForm: boolean;
}

export default class BitbucketProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      selectedAlmInstance: props.almInstances[0],
      loading: false,
      searching: false,
      showPersonalAccessTokenForm: true,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.almInstances.length === 0 && this.props.almInstances.length > 0) {
      this.setState({ selectedAlmInstance: this.props.almInstances[0] }, () => {
        this.fetchInitialData().catch(() => {
          /* noop */
        });
      });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchInitialData = async () => {
    const { showPersonalAccessTokenForm } = this.state;

    if (!showPersonalAccessTokenForm) {
      this.setState({ loading: true });
      const projects = await this.fetchBitbucketProjects().catch(() => undefined);

      let projectRepositories;
      if (projects && projects.length > 0) {
        projectRepositories = await this.fetchBitbucketRepositories(projects).catch(
          () => undefined,
        );
      }

      if (this.mounted) {
        this.setState({
          projects,
          projectRepositories,
          loading: false,
        });
      }
    }
  };

  fetchBitbucketProjects = (): Promise<BitbucketProject[] | undefined> => {
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance) {
      return Promise.resolve(undefined);
    }

    return getBitbucketServerProjects(selectedAlmInstance.key).then(({ projects }) => projects);
  };

  fetchBitbucketRepositories = (
    projects: BitbucketProject[],
  ): Promise<BitbucketProjectRepositories | undefined> => {
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance) {
      return Promise.resolve(undefined);
    }

    return Promise.all(
      projects.map((p) => {
        return getBitbucketServerRepositories(selectedAlmInstance.key, p.name).then(
          ({ isLastPage, repositories }) => {
            // Because the WS uses the project name rather than its key to find
            // repositories, we can match more repositories than we expect. For
            // example, p.name = "A1" would find repositories for projects "A1",
            // "A10", "A11", etc. This is a limitation of BBS. To make sure we
            // don't display incorrect information, filter on the project key.
            const filteredRepositories = repositories.filter((r) => r.projectKey === p.key);

            // And because of the above, the "isLastPage" cannot be relied upon
            // either. This one is impossible to get 100% for now. We can only
            // make some assumptions: by default, the page size for BBS is 25
            // (this is not part of the payload, so we don't know the actual
            // number; but changing this implies changing some advanced config,
            // so it's not likely). If the filtered repos is larger than this
            // number AND isLastPage is false, we'll keep it at false.
            // Otherwise, we assume it's true.
            const realIsLastPage =
              isLastPage || filteredRepositories.length < DEFAULT_BBS_PAGE_SIZE;

            return {
              repositories: filteredRepositories,
              isLastPage: realIsLastPage,
              projectKey: p.key,
            };
          },
        );
      }),
    ).then((results) => {
      return results.reduce(
        (acc: BitbucketProjectRepositories, { isLastPage, projectKey, repositories }) => {
          return { ...acc, [projectKey]: { allShown: isLastPage, repositories } };
        },
        {},
      );
    });
  };

  cleanUrl = () => {
    const { location, router } = this.props;
    delete location.query.resetPat;
    router.replace(location);
  };

  handlePersonalAccessTokenCreated = () => {
    this.cleanUrl();

    this.setState({ showPersonalAccessTokenForm: false }, () => {
      this.fetchInitialData();
    });
  };

  handleImportRepository = (selectedRepository: BitbucketRepository) => {
    const { selectedAlmInstance } = this.state;

    if (selectedAlmInstance) {
      this.props.onProjectSetupDone({
        creationMode: CreateProjectModes.BitbucketServer,
        almSetting: selectedAlmInstance.key,
        projects: [
          {
            projectKey: selectedRepository.projectKey,
            repositorySlug: selectedRepository.slug,
          },
        ],
      });
    }
  };

  handleSearch = (query: string) => {
    const { selectedAlmInstance } = this.state;

    if (!selectedAlmInstance) {
      return;
    }

    if (!query) {
      this.setState({ searching: false, searchResults: undefined });
      return;
    }

    this.setState({ searching: true });
    searchForBitbucketServerRepositories(selectedAlmInstance.key, query)
      .then(({ repositories }) => {
        if (this.mounted) {
          this.setState({ searching: false, searchResults: repositories });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ searching: false });
        }
      });
  };

  onSelectedAlmInstanceChange = (instance: AlmSettingsInstance) => {
    this.setState({
      selectedAlmInstance: instance,
      showPersonalAccessTokenForm: true,
      searching: false,
      searchResults: undefined,
    });
  };

  render() {
    const { canAdmin, loadingBindings, location, almInstances } = this.props;
    const {
      selectedAlmInstance,
      loading,
      projectRepositories,
      projects,
      searching,
      searchResults,
      showPersonalAccessTokenForm,
    } = this.state;

    return (
      <BitbucketCreateProjectRenderer
        selectedAlmInstance={selectedAlmInstance}
        almInstances={almInstances}
        canAdmin={canAdmin}
        loading={loading || loadingBindings}
        onImportRepository={this.handleImportRepository}
        onPersonalAccessTokenCreated={this.handlePersonalAccessTokenCreated}
        onSearch={this.handleSearch}
        onSelectedAlmInstanceChange={this.onSelectedAlmInstanceChange}
        projectRepositories={projectRepositories}
        projects={projects}
        resetPat={Boolean(location.query.resetPat)}
        searchResults={searchResults}
        searching={searching}
        showPersonalAccessTokenForm={
          showPersonalAccessTokenForm || Boolean(location.query.resetPat)
        }
      />
    );
  }
}
