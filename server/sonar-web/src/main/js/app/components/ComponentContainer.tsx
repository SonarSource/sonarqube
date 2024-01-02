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
import { differenceBy } from 'lodash';
import * as React from 'react';
import { Outlet } from 'react-router-dom';
import { getProjectAlmBinding, validateProjectAlmBinding } from '../../api/alm-settings';
import { getBranches, getPullRequests } from '../../api/branches';
import { getAnalysisStatus, getTasksForComponent } from '../../api/ce';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/navigation';
import { Location, Router, withRouter } from '../../components/hoc/withRouter';
import {
  getBranchLikeQuery,
  isBranch,
  isMainBranch,
  isPullRequest,
} from '../../helpers/branch-like';
import { HttpStatus } from '../../helpers/request';
import { getPortfolioUrl } from '../../helpers/urls';
import {
  ProjectAlmBindingConfigurationErrors,
  ProjectAlmBindingResponse,
} from '../../types/alm-settings';
import { BranchLike } from '../../types/branch-like';
import { ComponentQualifier, isPortfolioLike } from '../../types/component';
import { Feature } from '../../types/features';
import { Task, TaskStatuses, TaskTypes, TaskWarning } from '../../types/tasks';
import { Component, Status } from '../../types/types';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from './available-features/withAvailableFeatures';
import withBranchStatusActions from './branch-status/withBranchStatusActions';
import ComponentContainerNotFound from './ComponentContainerNotFound';
import { ComponentContext } from './componentContext/ComponentContext';
import PageUnavailableDueToIndexation from './indexation/PageUnavailableDueToIndexation';
import ComponentNav from './nav/component/ComponentNav';

interface Props extends WithAvailableFeaturesProps {
  location: Location;
  updateBranchStatus: (branchLike: BranchLike, component: string, status: Status) => void;
  router: Router;
}

interface State {
  branchLike?: BranchLike;
  branchLikes: BranchLike[];
  component?: Component;
  currentTask?: Task;
  isPending: boolean;
  loading: boolean;
  projectBinding?: ProjectAlmBindingResponse;
  projectBindingErrors?: ProjectAlmBindingConfigurationErrors;
  tasksInProgress?: Task[];
  warnings: TaskWarning[];
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

  fetchComponent = async () => {
    const { branch, id: key, pullRequest } = this.props.location.query;
    this.setState({ loading: true });

    let componentWithQualifier;
    try {
      const [nav, { component }] = await Promise.all([
        getComponentNavigation({ component: key, branch, pullRequest }),
        getComponentData({ component: key, branch, pullRequest }),
      ]);
      componentWithQualifier = this.addQualifier({ ...nav, ...component });
    } catch (e) {
      if (this.mounted) {
        if (e && e instanceof Response && e.status === HttpStatus.Forbidden) {
          handleRequiredAuthorization();
        } else {
          this.setState({ component: undefined, loading: false });
        }
      }
      return;
    }

    /*
     * There used to be a redirect from /dashboard to /portfolio which caused issues.
     * Links should be fixed to not rely on this redirect, but:
     * This is a fail-safe in case there are still some faulty links remaining.
     */
    if (
      this.props.location.pathname.match('dashboard') &&
      isPortfolioLike(componentWithQualifier.qualifier)
    ) {
      this.props.router.replace(getPortfolioUrl(componentWithQualifier.key));
    }

    const { branchLike, branchLikes } = await this.fetchBranches(componentWithQualifier);

    let projectBinding;
    if (componentWithQualifier.qualifier === ComponentQualifier.Project) {
      projectBinding = await getProjectAlmBinding(key).catch(() => undefined);
    }

    if (this.mounted) {
      this.setState({
        branchLike,
        branchLikes,
        component: componentWithQualifier,
        projectBinding,
        loading: false,
      });

      this.fetchStatus(componentWithQualifier.key);
      this.fetchWarnings(componentWithQualifier, branchLike);
      this.fetchProjectBindingErrors(componentWithQualifier);
    }
  };

  fetchBranches = async (componentWithQualifier: Component) => {
    const { hasFeature } = this.props;

    const breadcrumb = componentWithQualifier.breadcrumbs.find(({ qualifier }) => {
      return ([ComponentQualifier.Application, ComponentQualifier.Project] as string[]).includes(
        qualifier
      );
    });

    let branchLike = undefined;
    let branchLikes: BranchLike[] = [];

    if (breadcrumb) {
      const { key } = breadcrumb;
      const [branches, pullRequests] = await Promise.all([
        getBranches(key),
        !hasFeature(Feature.BranchSupport) ||
        breadcrumb.qualifier === ComponentQualifier.Application
          ? Promise.resolve([])
          : getPullRequests(key),
      ]);

      branchLikes = [...branches, ...pullRequests];
      branchLike = this.getCurrentBranchLike(branchLikes);

      this.registerBranchStatuses(branchLikes, componentWithQualifier);
    }

    return { branchLike, branchLikes };
  };

  fetchStatus = (componentKey: string) => {
    getTasksForComponent(componentKey).then(
      ({ current, queue }) => {
        if (this.mounted) {
          let shouldFetchComponent = false;
          this.setState(
            ({ branchLike, component, currentTask, tasksInProgress }) => {
              const newCurrentTask = this.getCurrentTask(current, branchLike);
              const pendingTasks = this.getPendingTasksForBranchLike(queue, branchLike);
              const newTasksInProgress = this.getInProgressTasks(pendingTasks);

              shouldFetchComponent = this.computeShouldFetchComponent(
                tasksInProgress,
                newTasksInProgress,
                currentTask,
                newCurrentTask,
                component
              );

              if (this.needsAnotherCheck(shouldFetchComponent, component, newTasksInProgress)) {
                // Refresh the status as long as there is tasks in progress or no analysis
                window.clearTimeout(this.watchStatusTimer);
                this.watchStatusTimer = window.setTimeout(
                  () => this.fetchStatus(componentKey),
                  FETCH_STATUS_WAIT_TIME
                );
              }

              const isPending = pendingTasks.some((task) => task.status === TaskStatuses.Pending);
              return {
                currentTask: newCurrentTask,
                isPending,
                tasksInProgress: newTasksInProgress,
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

  fetchWarnings = (component: Component, branchLike?: BranchLike) => {
    if (component.qualifier === ComponentQualifier.Project) {
      getAnalysisStatus({
        component: component.key,
        ...getBranchLikeQuery(branchLike),
      }).then(
        ({ component }) => {
          this.setState({ warnings: component.warnings });
        },
        () => {}
      );
    }
  };

  fetchProjectBindingErrors = async (component: Component) => {
    if (
      component.qualifier === ComponentQualifier.Project &&
      component.analysisDate === undefined &&
      this.props.hasFeature(Feature.BranchSupport)
    ) {
      const projectBindingErrors = await validateProjectAlmBinding(component.key).catch(
        () => undefined
      );
      if (this.mounted) {
        this.setState({ projectBindingErrors });
      }
    }
  };

  addQualifier = (component: Component) => ({
    ...component,
    qualifier: component.breadcrumbs[component.breadcrumbs.length - 1].qualifier,
  });

  getCurrentBranchLike = (branchLikes: BranchLike[]) => {
    const { query } = this.props.location;
    return query.pullRequest
      ? branchLikes.find((b) => isPullRequest(b) && b.key === query.pullRequest)
      : branchLikes.find((b) => isBranch(b) && (query.branch ? b.name === query.branch : b.isMain));
  };

  getCurrentTask = (current: Task, branchLike?: BranchLike) => {
    if (!current || !this.isReportRelatedTask(current)) {
      return undefined;
    }

    return current.status === TaskStatuses.Failed || this.isSameBranch(current, branchLike)
      ? current
      : undefined;
  };

  getPendingTasksForBranchLike = (pendingTasks: Task[], branchLike?: BranchLike) => {
    return pendingTasks.filter(
      (task) => this.isReportRelatedTask(task) && this.isSameBranch(task, branchLike)
    );
  };

  getInProgressTasks = (pendingTasks: Task[]) => {
    return pendingTasks.filter((task) => task.status === TaskStatuses.InProgress);
  };

  isReportRelatedTask = (task: Task) => {
    return [TaskTypes.AppRefresh, TaskTypes.Report, TaskTypes.ViewRefresh].includes(task.type);
  };

  computeShouldFetchComponent = (
    tasksInProgress: Task[] | undefined,
    newTasksInProgress: Task[],
    currentTask: Task | undefined,
    newCurrentTask: Task | undefined,
    component: Component | undefined
  ) => {
    const progressHasChanged = Boolean(
      tasksInProgress &&
        (newTasksInProgress.length !== tasksInProgress.length ||
          differenceBy(newTasksInProgress, tasksInProgress, 'id').length > 0)
    );

    const currentTaskHasChanged = Boolean(
      (!currentTask && newCurrentTask) ||
        (currentTask && newCurrentTask && currentTask.id !== newCurrentTask.id)
    );

    if (progressHasChanged) {
      return true;
    } else if (currentTaskHasChanged && component) {
      // We return true if:
      // - there was no prior analysis date (means this is an empty project, and
      //   a new analysis came in)
      // - OR, there was a prior analysis date (non-empty project) AND there were
      //   some tasks in progress before
      return (
        Boolean(!component.analysisDate) ||
        Boolean(component.analysisDate && tasksInProgress?.length)
      );
    }
    return false;
  };

  needsAnotherCheck = (
    shouldFetchComponent: boolean,
    component: Component | undefined,
    newTasksInProgress: Task[]
  ) => {
    return (
      !shouldFetchComponent &&
      component &&
      (newTasksInProgress.length > 0 || !component.analysisDate)
    );
  };

  isSameBranch = (task: Pick<Task, 'branch' | 'pullRequest'>, branchLike?: BranchLike) => {
    if (branchLike) {
      if (isMainBranch(branchLike)) {
        return (!task.pullRequest && !task.branch) || branchLike.name === task.branch;
      }
      if (isPullRequest(branchLike)) {
        return branchLike.key === task.pullRequest;
      }
      if (isBranch(branchLike)) {
        return branchLike.name === task.branch;
      }
    }
    return !task.branch && !task.pullRequest;
  };

  registerBranchStatuses = (branchLikes: BranchLike[], component: Component) => {
    branchLikes.forEach((branchLike) => {
      if (branchLike.status) {
        this.props.updateBranchStatus(
          branchLike,
          component.key,
          branchLike.status.qualityGateStatus
        );
      }
    });
  };

  handleComponentChange = (changes: Partial<Component>) => {
    if (this.mounted) {
      this.setState((state) => {
        if (state.component) {
          const newComponent: Component = { ...state.component, ...changes };
          return { component: newComponent };
        }
        return null;
      });
    }
  };

  handleBranchesChange = () => {
    const { router, location } = this.props;
    const { component } = this.state;

    if (this.mounted && component) {
      this.fetchBranches(component).then(
        ({ branchLike, branchLikes }) => {
          if (this.mounted) {
            this.setState({ branchLike, branchLikes });

            if (branchLike === undefined) {
              router.replace({ query: { ...location.query, branch: undefined } });
            }
          }
        },
        () => {}
      );
    }
  };

  handleWarningDismiss = () => {
    const { component } = this.state;
    if (component !== undefined) {
      this.fetchWarnings(component);
    }
  };

  render() {
    const { component, loading } = this.state;

    if (!loading && !component) {
      return <ComponentContainerNotFound />;
    }

    if (component?.needIssueSync) {
      return <PageUnavailableDueToIndexation component={component} />;
    }

    const {
      branchLike,
      branchLikes,
      currentTask,
      isPending,
      projectBinding,
      projectBindingErrors,
      tasksInProgress,
      warnings,
    } = this.state;
    const isInProgress = tasksInProgress && tasksInProgress.length > 0;

    return (
      <div>
        {component &&
          !([ComponentQualifier.File, ComponentQualifier.TestFile] as string[]).includes(
            component.qualifier
          ) && (
            <ComponentNav
              branchLikes={branchLikes}
              component={component}
              currentBranchLike={branchLike}
              currentTask={currentTask}
              currentTaskOnSameBranch={currentTask && this.isSameBranch(currentTask, branchLike)}
              isInProgress={isInProgress}
              isPending={isPending}
              onComponentChange={this.handleComponentChange}
              onWarningDismiss={this.handleWarningDismiss}
              projectBinding={projectBinding}
              projectBindingErrors={projectBindingErrors}
              warnings={warnings}
            />
          )}
        {loading ? (
          <div className="page page-limited">
            <i className="spinner" />
          </div>
        ) : (
          <ComponentContext.Provider
            value={{
              branchLike,
              branchLikes,
              component,
              isInProgress,
              isPending,
              onBranchesChange: this.handleBranchesChange,
              onComponentChange: this.handleComponentChange,
              projectBinding,
            }}
          >
            <Outlet />
          </ComponentContext.Provider>
        )}
      </div>
    );
  }
}

export default withRouter(withAvailableFeatures(withBranchStatusActions(ComponentContainer)));
