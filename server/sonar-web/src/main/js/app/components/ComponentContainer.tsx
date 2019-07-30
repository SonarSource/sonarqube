/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { differenceBy } from 'lodash';
import * as React from 'react';
import { connect } from 'react-redux';
import { getBranches, getPullRequests } from '../../api/branches';
import { getAnalysisStatus, getTasksForComponent } from '../../api/ce';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/nav';
import { STATUSES } from '../../apps/background-tasks/constants';
import { Location, Router, withRouter } from '../../components/hoc/withRouter';
import {
  getBranchLikeQuery,
  isBranch,
  isLongLivingBranch,
  isMainBranch,
  isPullRequest,
  isShortLivingBranch
} from '../../helpers/branches';
import { isSonarCloud } from '../../helpers/system';
import {
  fetchOrganization,
  registerBranchStatus,
  requireAuthorization
} from '../../store/rootActions';
import ComponentContainerNotFound from './ComponentContainerNotFound';
import { ComponentContext } from './ComponentContext';
import ComponentNav from './nav/component/ComponentNav';

interface Props {
  children: React.ReactElement;
  fetchOrganization: (organization: string) => void;
  location: Pick<Location, 'query'>;
  registerBranchStatus: (branchLike: T.BranchLike, component: string, status: T.Status) => void;
  requireAuthorization: (router: Pick<Router, 'replace'>) => void;
  router: Pick<Router, 'replace'>;
}

interface State {
  branchLike?: T.BranchLike;
  branchLikes: T.BranchLike[];
  component?: T.Component;
  currentTask?: T.Task;
  isPending: boolean;
  loading: boolean;
  tasksInProgress?: T.Task[];
  warnings: string[];
}

const FETCH_STATUS_WAIT_TIME = 3000;

export class ComponentContainer extends React.PureComponent<Props, State> {
  watchStatusTimer?: number;
  mounted = false;
  state: State = { branchLikes: [], isPending: false, loading: true, warnings: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.location.query.id !== this.props.location.query.id ||
      prevProps.location.query.branch !== this.props.location.query.branch ||
      prevProps.location.query.pullRequest !== this.props.location.query.pullRequest
    ) {
      this.fetchComponent();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    window.clearTimeout(this.watchStatusTimer);
  }

  addQualifier = (component: T.Component) => ({
    ...component,
    qualifier: component.breadcrumbs[component.breadcrumbs.length - 1].qualifier
  });

  fetchComponent() {
    const { branch, id: key, pullRequest } = this.props.location.query;
    this.setState({ loading: true });

    const onError = (response?: Response) => {
      if (this.mounted) {
        if (response && response.status === 403) {
          this.props.requireAuthorization(this.props.router);
        } else {
          this.setState({ component: undefined, loading: false });
        }
      }
    };

    Promise.all([
      getComponentNavigation({ component: key, branch, pullRequest }),
      getComponentData({ component: key, branch, pullRequest })
    ])
      .then(([nav, { component }]) => {
        const componentWithQualifier = this.addQualifier({ ...nav, ...component });

        if (isSonarCloud()) {
          this.props.fetchOrganization(componentWithQualifier.organization);
        }
        return componentWithQualifier;
      }, onError)
      .then(this.fetchBranches)
      .then(
        ({ branchLike, branchLikes, component }) => {
          if (this.mounted) {
            this.setState({
              branchLike,
              branchLikes,
              component,
              loading: false
            });
            this.fetchStatus(component);
            this.fetchWarnings(component, branchLike);
          }
        },
        () => {}
      );
  }

  fetchBranches = (
    component: T.Component
  ): Promise<{
    branchLike?: T.BranchLike;
    branchLikes: T.BranchLike[];
    component: T.Component;
  }> => {
    const breadcrumb = component.breadcrumbs.find(({ qualifier }) => {
      return ['APP', 'TRK'].includes(qualifier);
    });

    if (breadcrumb) {
      const { key } = breadcrumb;
      return Promise.all([
        getBranches(key),
        breadcrumb.qualifier === 'APP' ? Promise.resolve([]) : getPullRequests(key)
      ]).then(([branches, pullRequests]) => {
        const branchLikes = [...branches, ...pullRequests];
        const branchLike = this.getCurrentBranchLike(branchLikes);

        this.registerBranchStatuses(branchLikes, component);

        return { branchLike, branchLikes, component };
      });
    } else {
      return Promise.resolve({ branchLikes: [], component });
    }
  };

  fetchStatus = (component: T.Component) => {
    getTasksForComponent(component.key).then(
      ({ current, queue }) => {
        if (this.mounted) {
          let shouldFetchComponent = false;
          this.setState(
            ({ branchLike, component, currentTask, tasksInProgress }) => {
              const newCurrentTask = this.getCurrentTask(current, branchLike);
              const pendingTasks = this.getPendingTasks(queue, branchLike);
              const newTasksInProgress = pendingTasks.filter(
                task => task.status === STATUSES.IN_PROGRESS
              );

              const currentTaskChanged =
                currentTask && newCurrentTask && currentTask.id !== newCurrentTask.id;
              const progressChanged =
                tasksInProgress &&
                (newTasksInProgress.length !== tasksInProgress.length ||
                  differenceBy(newTasksInProgress, tasksInProgress, 'id').length > 0);

              shouldFetchComponent = Boolean(currentTaskChanged || progressChanged);
              if (
                !shouldFetchComponent &&
                component &&
                (newTasksInProgress.length > 0 || !component.analysisDate)
              ) {
                // Refresh the status as long as there is tasks in progress or no analysis
                window.clearTimeout(this.watchStatusTimer);
                this.watchStatusTimer = window.setTimeout(
                  () => this.fetchStatus(component),
                  FETCH_STATUS_WAIT_TIME
                );
              }

              const isPending = pendingTasks.some(task => task.status === STATUSES.PENDING);
              return {
                currentTask: newCurrentTask,
                isPending,
                tasksInProgress: newTasksInProgress
              };
            },
            () => {
              if (shouldFetchComponent) {
                this.fetchComponent();
              }
            }
          );
        }
      },
      () => {}
    );
  };

  fetchWarnings = (component: T.Component, branchLike?: T.BranchLike) => {
    if (component.qualifier === 'TRK') {
      getAnalysisStatus({
        component: component.key,
        ...getBranchLikeQuery(branchLike)
      }).then(
        ({ component }) => {
          this.setState({ warnings: component.warnings });
        },
        () => {}
      );
    }
  };

  getCurrentBranchLike = (branchLikes: T.BranchLike[]) => {
    const { query } = this.props.location;
    return query.pullRequest
      ? branchLikes.find(b => isPullRequest(b) && b.key === query.pullRequest)
      : branchLikes.find(b => isBranch(b) && (query.branch ? b.name === query.branch : b.isMain));
  };

  getCurrentTask = (current: T.Task, branchLike?: T.BranchLike) => {
    if (!current) {
      return undefined;
    }

    return current.status === STATUSES.FAILED || this.isSameBranch(current, branchLike)
      ? current
      : undefined;
  };

  getPendingTasks = (pendingTasks: T.Task[], branchLike?: T.BranchLike) => {
    return pendingTasks.filter(task => this.isSameBranch(task, branchLike));
  };

  isSameBranch = (
    task: Pick<T.Task, 'branch' | 'branchType' | 'pullRequest'>,
    branchLike?: T.BranchLike
  ) => {
    if (branchLike && !isMainBranch(branchLike)) {
      if (isPullRequest(branchLike)) {
        return branchLike.key === task.pullRequest;
      }
      if (isShortLivingBranch(branchLike) || isLongLivingBranch(branchLike)) {
        return branchLike.type === task.branchType && branchLike.name === task.branch;
      }
    }
    return !task.branch && !task.pullRequest;
  };

  registerBranchStatuses = (branchLikes: T.BranchLike[], component: T.Component) => {
    branchLikes.forEach(branchLike => {
      if (branchLike.status) {
        this.props.registerBranchStatus(
          branchLike,
          component.key,
          branchLike.status.qualityGateStatus
        );
      }
    });
  };

  handleComponentChange = (changes: Partial<T.Component>) => {
    if (this.mounted) {
      this.setState(state => {
        if (state.component) {
          const newComponent: T.Component = { ...state.component, ...changes };
          return { component: newComponent };
        }
        return null;
      });
    }
  };

  handleBranchesChange = () => {
    if (this.mounted && this.state.component) {
      this.fetchBranches(this.state.component).then(
        ({ branchLike, branchLikes }) => {
          if (this.mounted) {
            this.setState({ branchLike, branchLikes });
          }
        },
        () => {}
      );
    }
  };

  render() {
    const { component, loading } = this.state;

    if (!loading && !component) {
      return <ComponentContainerNotFound />;
    }

    const { branchLike, branchLikes, currentTask, isPending, tasksInProgress } = this.state;
    const isInProgress = tasksInProgress && tasksInProgress.length > 0;

    return (
      <div>
        {component && !['FIL', 'UTS'].includes(component.qualifier) && (
          <ComponentNav
            branchLikes={branchLikes}
            component={component}
            currentBranchLike={branchLike}
            currentTask={currentTask}
            currentTaskOnSameBranch={currentTask && this.isSameBranch(currentTask, branchLike)}
            isInProgress={isInProgress}
            isPending={isPending}
            location={this.props.location}
            warnings={this.state.warnings}
          />
        )}
        {loading ? (
          <div className="page page-limited">
            <i className="spinner" />
          </div>
        ) : (
          <ComponentContext.Provider value={{ branchLike, component }}>
            {React.cloneElement(this.props.children, {
              branchLike,
              branchLikes,
              component,
              isInProgress,
              isPending,
              onBranchesChange: this.handleBranchesChange,
              onComponentChange: this.handleComponentChange
            })}
          </ComponentContext.Provider>
        )}
      </div>
    );
  }
}

const mapDispatchToProps = { fetchOrganization, registerBranchStatus, requireAuthorization };

export default withRouter(
  connect(
    null,
    mapDispatchToProps
  )(ComponentContainer)
);
