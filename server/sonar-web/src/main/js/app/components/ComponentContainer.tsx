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

import { CenteredLayout, Spinner } from 'design-system';
import { differenceBy } from 'lodash';
import * as React from 'react';
import { createPortal } from 'react-dom';
import { Helmet } from 'react-helmet-async';
import { Outlet } from 'react-router-dom';
import { validateProjectAlmBinding } from '../../api/alm-settings';
import { getTasksForComponent } from '../../api/ce';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/navigation';
import { Location, Router, withRouter } from '../../components/hoc/withRouter';
import { translateWithParameters } from '../../helpers/l10n';
import { HttpStatus } from '../../helpers/request';
import { getPortfolioUrl, getProjectUrl } from '../../helpers/urls';
import { ProjectAlmBindingConfigurationErrors } from '../../types/alm-settings';
import { ComponentQualifier, isFile, isPortfolioLike } from '../../types/component';
import { Feature } from '../../types/features';
import { Task, TaskStatuses, TaskTypes } from '../../types/tasks';
import { Component } from '../../types/types';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';
import ComponentContainerNotFound from './ComponentContainerNotFound';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from './available-features/withAvailableFeatures';
import { ComponentContext } from './componentContext/ComponentContext';
import ComponentNav from './nav/component/ComponentNav';

export interface Props extends WithAvailableFeaturesProps {
  location: Location;
  router: Router;
}

interface State {
  component?: Component;
  currentTask?: Task;
  isPending: boolean;
  loading: boolean;
  projectBindingErrors?: ProjectAlmBindingConfigurationErrors;
  tasksInProgress?: Task[];
}

const FETCH_STATUS_WAIT_TIME = 3000;

export class ComponentContainer extends React.PureComponent<Props, State> {
  watchStatusTimer?: number;
  mounted = false;
  state: State = { isPending: false, loading: true };
  portalAnchor: Element | null = null;

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent();

    this.portalAnchor = document.querySelector('#component-nav-portal');
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

  redirectIfNeeded = (componentWithQualifier: { key: string; qualifier: string }) => {
    /*
     * There used to be a redirect from /dashboard to /portfolio which caused issues.
     * Links should be fixed to not rely on this redirect, but:
     * This is a fail-safe in case there are still some faulty links remaining.
     */
    if (
      this.props.location.pathname.includes('dashboard') &&
      isPortfolioLike(componentWithQualifier.qualifier)
    ) {
      this.props.router.replace(getPortfolioUrl(componentWithQualifier.key));
    }
  };

  fetchComponent = async (shouldRedirectToDashboard = false) => {
    const { branch, id: key, pullRequest } = this.props.location.query;
    const { component } = this.state;

    // Only show loader if we're changing components
    if (component?.key !== key) {
      this.setState({ loading: true });
    }

    let componentWithQualifier;
    try {
      const [nav, { component }] = await Promise.all([
        getComponentNavigation({ component: key, branch, pullRequest }),
        getComponentData({ component: key, branch, pullRequest }),
      ]);

      componentWithQualifier = this.addQualifier({ ...nav, ...component });
    } catch (e) {
      if (this.mounted) {
        if (e instanceof Response && e.status === HttpStatus.Forbidden) {
          handleRequiredAuthorization();
        } else {
          this.setState({ component: undefined, loading: false });
        }
      }

      return;
    }

    this.redirectIfNeeded(componentWithQualifier);

    if (this.mounted) {
      this.setState(
        {
          component: componentWithQualifier,
          loading: false,
        },
        () => {
          if (shouldRedirectToDashboard && this.props.location.pathname.includes('tutorials')) {
            this.props.router.replace(getProjectUrl(key));
          }
        },
      );

      this.fetchStatus(componentWithQualifier.key);
      this.fetchProjectBindingErrors(componentWithQualifier);
    }
  };

  fetchStatus = (componentKey: string) => {
    getTasksForComponent(componentKey).then(
      ({ current, queue }) => {
        if (this.mounted) {
          let shouldFetchComponent = false;
          let shouldRedirectToDashboard = false;
          this.setState(
            ({ component, currentTask, tasksInProgress }) => {
              const newCurrentTask = this.getCurrentTask(current);
              const pendingTasks = this.getPendingTasksForBranchLike(queue);
              const newTasksInProgress = this.getInProgressTasks(pendingTasks);

              shouldFetchComponent = this.computeShouldFetchComponent(
                tasksInProgress,
                newTasksInProgress,
                currentTask,
                newCurrentTask,
                component,
              );

              shouldRedirectToDashboard =
                component !== undefined && Boolean(!component.analysisDate);

              if (this.needsAnotherCheck(shouldFetchComponent, component, newTasksInProgress)) {
                // Refresh the status as long as there is tasks in progress or no analysis
                window.clearTimeout(this.watchStatusTimer);

                this.watchStatusTimer = window.setTimeout(
                  () => this.fetchStatus(componentKey),
                  FETCH_STATUS_WAIT_TIME,
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
                this.fetchComponent(shouldRedirectToDashboard);
              }
            },
          );
        }
      },
      () => {},
    );
  };

  fetchProjectBindingErrors = async (component: Component) => {
    if (
      component.qualifier === ComponentQualifier.Project &&
      component.analysisDate === undefined &&
      this.props.hasFeature(Feature.BranchSupport)
    ) {
      const projectBindingErrors = await validateProjectAlmBinding(component.key).catch(
        () => undefined,
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

  getCurrentTask = (current?: Task) => {
    if (!current || !this.isReportRelatedTask(current)) {
      return undefined;
    }

    return current.status === TaskStatuses.Failed || this.isSameBranch(current)
      ? current
      : undefined;
  };

  getPendingTasksForBranchLike = (pendingTasks: Task[]) => {
    return pendingTasks.filter((task) => this.isReportRelatedTask(task) && this.isSameBranch(task));
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
    component: Component | undefined,
  ) => {
    const progressHasChanged = Boolean(
      tasksInProgress &&
        (newTasksInProgress.length !== tasksInProgress.length ||
          differenceBy(newTasksInProgress, tasksInProgress, 'id').length > 0),
    );

    const currentTaskHasChanged = Boolean(
      (!currentTask && newCurrentTask) ||
        (currentTask && newCurrentTask && currentTask.id !== newCurrentTask.id),
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
    newTasksInProgress: Task[],
  ) => {
    return (
      !shouldFetchComponent &&
      component &&
      (newTasksInProgress.length > 0 || !component.analysisDate)
    );
  };

  isSameBranch = (task: Pick<Task, 'branch' | 'pullRequest'>) => {
    const { branch, pullRequest } = this.props.location.query;

    if (!pullRequest && !branch) {
      return !task.branch && !task.pullRequest;
    }

    if (pullRequest) {
      return pullRequest === task.pullRequest;
    }

    if (branch) {
      return branch === task.branch;
    }

    return false;
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

  render() {
    const { component, loading } = this.state;

    if (!loading && !component) {
      return (
        <ComponentContainerNotFound
          isPortfolioLike={this.props.location.pathname.includes('portfolio')}
        />
      );
    }

    const { currentTask, isPending, projectBindingErrors, tasksInProgress } = this.state;

    const isInProgress = tasksInProgress && tasksInProgress.length > 0;

    return (
      <div>
        <Helmet
          defer={false}
          titleTemplate={translateWithParameters(
            'page_title.template.with_instance',
            component?.name ?? '',
          )}
        />
        {component &&
          !isFile(component.qualifier) &&
          this.portalAnchor &&
          /* Use a portal to fix positioning until we can fully review the layout */
          createPortal(
            <ComponentNav
              component={component}
              currentTask={currentTask}
              isInProgress={isInProgress}
              isPending={isPending}
              projectBindingErrors={projectBindingErrors}
            />,
            this.portalAnchor,
          )}
        {loading ? (
          <CenteredLayout>
            <Spinner className="sw-mt-10" />
          </CenteredLayout>
        ) : (
          <ComponentContext.Provider
            value={{
              component,
              isInProgress,
              isPending,
              onComponentChange: this.handleComponentChange,
              fetchComponent: this.fetchComponent,
            }}
          >
            <Outlet />
          </ComponentContext.Provider>
        )}
      </div>
    );
  }
}

export default withRouter(withAvailableFeatures(ComponentContainer));
