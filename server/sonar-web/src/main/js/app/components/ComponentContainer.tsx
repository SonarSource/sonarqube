/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { differenceBy } from 'lodash';
import ComponentContainerNotFound from './ComponentContainerNotFound';
import ComponentNav from './nav/component/ComponentNav';
import { Component, BranchLike } from '../types';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';
import { getBranches, getPullRequests } from '../../api/branches';
import { Task, getTasksForComponent, PendingTask } from '../../api/ce';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/nav';
import { fetchOrganizations } from '../../store/rootActions';
import { STATUSES } from '../../apps/background-tasks/constants';
import {
  isPullRequest,
  isBranch,
  isMainBranch,
  isLongLivingBranch,
  isShortLivingBranch
} from '../../helpers/branches';

interface Props {
  children: any;
  fetchOrganizations: (organizations: string[]) => void;
  location: {
    query: { branch?: string; id: string; pullRequest?: string };
  };
}

interface State {
  branchLike?: BranchLike;
  branchLikes: BranchLike[];
  component?: Component;
  currentTask?: Task;
  isPending: boolean;
  loading: boolean;
  tasksInProgress?: PendingTask[];
}

const FETCH_STATUS_WAIT_TIME = 3000;

export class ComponentContainer extends React.PureComponent<Props, State> {
  watchStatusTimer?: number;
  mounted = false;

  static contextTypes = {
    organizationsEnabled: PropTypes.bool
  };

  constructor(props: Props) {
    super(props);
    this.state = { branchLikes: [], isPending: false, loading: true };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      nextProps.location.query.id !== this.props.location.query.id ||
      nextProps.location.query.branch !== this.props.location.query.branch ||
      nextProps.location.query.pullRequest !== this.props.location.query.pullRequest
    ) {
      this.fetchComponent(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  addQualifier = (component: Component) => ({
    ...component,
    qualifier: component.breadcrumbs[component.breadcrumbs.length - 1].qualifier
  });

  fetchComponent(props = this.props) {
    const { branch, id: key, pullRequest } = props.location.query;
    this.setState({ loading: true });

    const onError = (error: any) => {
      if (this.mounted) {
        if (error.response && error.response.status === 403) {
          handleRequiredAuthorization();
        } else {
          this.setState({ loading: false });
        }
      }
    };

    Promise.all([
      getComponentNavigation({ componentKey: key, branch, pullRequest }),
      getComponentData({ component: key, branch, pullRequest })
    ]).then(([nav, data]) => {
      const component = this.addQualifier({ ...nav, ...data });

      if (this.context.organizationsEnabled) {
        this.props.fetchOrganizations([component.organization]);
      }

      this.fetchBranches(component).then(({ branchLike, branchLikes }) => {
        if (this.mounted) {
          this.setState({ branchLike, branchLikes, component, loading: false });
          this.fetchStatus(component);
        }
      }, onError);
    }, onError);
  }

  fetchBranches = (
    component: Component
  ): Promise<{ branchLike?: BranchLike; branchLikes: BranchLike[] }> => {
    const application = component.breadcrumbs.find(({ qualifier }) => qualifier === 'APP');
    if (application) {
      return getBranches(application.key).then(branchLikes => {
        return {
          branchLike: this.getCurrentBranchLike(branchLikes),
          branchLikes
        };
      });
    }
    const project = component.breadcrumbs.find(({ qualifier }) => qualifier === 'TRK');
    return project
      ? Promise.all([getBranches(project.key), getPullRequests(project.key)]).then(
          ([branches, pullRequests]) => {
            const branchLikes = [...branches, ...pullRequests];
            return {
              branchLike: this.getCurrentBranchLike(branchLikes),
              branchLikes
            };
          }
        )
      : Promise.resolve({ branchLikes: [] });
  };

  fetchStatus = (component: Component) => {
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
              if (!shouldFetchComponent && component && newTasksInProgress.length > 0) {
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

  getCurrentBranchLike = (branchLikes: BranchLike[]) => {
    const { query } = this.props.location;
    return query.pullRequest
      ? branchLikes.find(b => isPullRequest(b) && b.key === query.pullRequest)
      : branchLikes.find(b => isBranch(b) && (query.branch ? b.name === query.branch : b.isMain));
  };

  getCurrentTask = (current: Task, branchLike?: BranchLike) => {
    if (!current) {
      return undefined;
    }

    return current.status === STATUSES.FAILED || this.isSameBranch(current, branchLike)
      ? current
      : undefined;
  };

  getPendingTasks = (pendingTasks: PendingTask[], branchLike?: BranchLike) => {
    return pendingTasks.filter(task => this.isSameBranch(task, branchLike));
  };

  isSameBranch = (
    task: Pick<PendingTask, 'branch' | 'branchType' | 'pullRequest'>,
    branchLike?: BranchLike
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

  handleComponentChange = (changes: {}) => {
    if (this.mounted) {
      this.setState(state => ({ component: { ...state.component, ...changes } }));
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
        {component &&
          !['FIL', 'UTS'].includes(component.qualifier) && (
            <ComponentNav
              branchLikes={branchLikes}
              component={component}
              currentBranchLike={branchLike}
              currentTask={currentTask}
              isInProgress={isInProgress}
              isPending={isPending}
              location={this.props.location}
            />
          )}
        {loading ? (
          <div className="page page-limited">
            <i className="spinner" />
          </div>
        ) : (
          React.cloneElement(this.props.children, {
            branchLike,
            branchLikes,
            component,
            isInProgress,
            isPending,
            onBranchesChange: this.handleBranchesChange,
            onComponentChange: this.handleComponentChange
          })
        )}
      </div>
    );
  }
}

const mapDispatchToProps = { fetchOrganizations };

export default connect<any, any, any>(null, mapDispatchToProps)(ComponentContainer);
