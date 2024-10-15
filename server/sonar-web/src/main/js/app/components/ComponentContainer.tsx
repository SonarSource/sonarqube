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
import { CenteredLayout, Spinner } from 'design-system';
import { differenceBy } from 'lodash';
import * as React from 'react';
import { createPortal } from 'react-dom';
import { Helmet } from 'react-helmet-async';
import { Outlet } from 'react-router-dom';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { validateProjectAlmBinding } from '../../api/alm-settings';
import { getTasksForComponent } from '../../api/ce';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/navigation';
import { translateWithParameters } from '../../helpers/l10n';
import { HttpStatus } from '../../helpers/request';
import { getPortfolioUrl, getProjectUrl, getPullRequestUrl } from '../../helpers/urls';
import { useCurrentBranchQuery } from '../../queries/branch';
import { useStandardExperienceMode } from '../../queries/settings';
import { ProjectAlmBindingConfigurationErrors } from '../../types/alm-settings';
import { Branch } from '../../types/branch-like';
import { isFile } from '../../types/component';
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

const FETCH_STATUS_WAIT_TIME = 3000;

function ComponentContainer({ hasFeature }: Readonly<WithAvailableFeaturesProps>) {
  const watchStatusTimer = React.useRef<number>();
  const portalAnchor = React.useRef<Element | null>(null);
  const oldTasksInProgress = React.useRef<Task[]>();
  const oldCurrentTask = React.useRef<Task>();
  const {
    query: { id: key, branch, pullRequest, fixedInPullRequest },
    pathname,
  } = useLocation();
  const router = useRouter();

  const [component, setComponent] = React.useState<Component>();
  const [currentTask, setCurrentTask] = React.useState<Task>();
  const [tasksInProgress, setTasksInProgress] = React.useState<Task[]>();
  const [projectBindingErrors, setProjectBindingErrors] =
    React.useState<ProjectAlmBindingConfigurationErrors>();
  const [loading, setLoading] = React.useState(true);
  const [isPending, setIsPending] = React.useState(false);
  const { data: branchLike, isFetching } = useCurrentBranchQuery(
    fixedInPullRequest ? component : undefined,
  );

  //prefetch isStandardExperienceMode
  useStandardExperienceMode();

  const isInTutorials = pathname.includes('tutorials');

  const fetchComponent = React.useCallback(
    async (branchName?: string) => {
      // Only show loader if we're changing components
      if (component?.key !== key) {
        setLoading(true);
      }
      let componentWithQualifier;
      const targetBranch = branch ?? branchName;
      try {
        const [nav, { component }] = await Promise.all([
          getComponentNavigation({ component: key, branch: targetBranch, pullRequest }),
          getComponentData({ component: key, branch: targetBranch, pullRequest }),
        ]);

        componentWithQualifier = addQualifier({ ...nav, ...component });
      } catch (e) {
        if (e instanceof Response && e.status === HttpStatus.Forbidden) {
          handleRequiredAuthorization();
        }
      } finally {
        setComponent(componentWithQualifier);
        setLoading(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [key, branch, pullRequest],
  );

  const fetchStatus = React.useCallback(
    async (componentKey: string) => {
      try {
        const { current, queue } = await getTasksForComponent(componentKey);
        const newCurrentTask = getCurrentTask(current, branch, pullRequest, isInTutorials);
        const pendingTasks = getReportRelatedPendingTasks(
          queue,
          branch,
          pullRequest,
          isInTutorials,
        );
        const newTasksInProgress = getInProgressTasks(pendingTasks);

        const isPending = pendingTasks.some((task) => task.status === TaskStatuses.Pending);

        setIsPending(isPending);
        setCurrentTask(newCurrentTask);
        setTasksInProgress(newTasksInProgress);
      } catch {
        // noop
      }
    },
    [branch, isInTutorials, pullRequest],
  );

  const fetchProjectBindingErrors = React.useCallback(
    async (component: Component) => {
      if (
        component.qualifier === ComponentQualifier.Project &&
        component.analysisDate === undefined &&
        hasFeature(Feature.BranchSupport)
      ) {
        try {
          const projectBindingErrors = await validateProjectAlmBinding(component.key);

          setProjectBindingErrors(projectBindingErrors);
        } catch {
          // noop
        }
      }
    },
    [hasFeature],
  );

  const handleComponentChange = React.useCallback(
    (changes: Partial<Component>) => {
      if (!component) {
        return;
      }

      setComponent({ ...component, ...changes });
    },
    [component],
  );

  React.useEffect(() => {
    if (key) {
      fetchComponent();
    }
  }, [key, fetchComponent]);

  // Fetch status and errors when component has changed
  React.useEffect(() => {
    if (component) {
      fetchStatus(component.key);
      fetchProjectBindingErrors(component);
    }
  }, [component, fetchStatus, fetchProjectBindingErrors]);

  // Refetch status when tasks in progress/current task have changed
  // Or refetch component based on computeHasUpdatedTasks
  React.useEffect(() => {
    // Stop here if tasks are not fetched yet
    if (!tasksInProgress) {
      return;
    }

    const tasks = tasksInProgress ?? [];
    const hasUpdatedTasks = computeHasUpdatedTasks(
      oldTasksInProgress.current,
      tasks,
      oldCurrentTask.current,
      currentTask,
      component,
    );

    if (isInTutorials && hasUpdatedTasks) {
      const { branch: branchName, pullRequest: pullRequestKey } = currentTask ?? tasks[0];
      const url =
        pullRequestKey !== undefined
          ? getPullRequestUrl(key, pullRequestKey)
          : getProjectUrl(key, branchName);
      router.replace(url);
    }

    if (needsAnotherCheck(hasUpdatedTasks, component, tasks)) {
      // Refresh the status as long as there are tasks in progress or no analysis
      window.clearTimeout(watchStatusTimer.current);

      watchStatusTimer.current = window.setTimeout(() => {
        fetchStatus(component?.key ?? '');
      }, FETCH_STATUS_WAIT_TIME);
    } else if (hasUpdatedTasks) {
      fetchComponent();
    }

    oldCurrentTask.current = currentTask;
    oldTasksInProgress.current = tasks;
  }, [
    component,
    currentTask,
    fetchComponent,
    fetchStatus,
    isInTutorials,
    key,
    router,
    tasksInProgress,
  ]);

  // Refetch component when a new branch is analyzed
  React.useEffect(() => {
    if (branchLike?.analysisDate && !component?.analysisDate) {
      fetchComponent();
    }
  }, [branchLike, component, fetchComponent]);

  // Refetch component when target branch for fixing pull request is fetched
  React.useEffect(() => {
    const branch = branchLike as Branch;

    if (fixedInPullRequest && !isFetching && branch && component?.branch !== branch.name) {
      fetchComponent(branch.name);
    }
  }, [fetchComponent, component, branchLike, fixedInPullRequest, isFetching]);

  // Redirects
  React.useEffect(() => {
    /*
     * There used to be a redirect from /dashboard to /portfolio which caused issues.
     * Links should be fixed to not rely on this redirect, but:
     * This is a fail-safe in case there are still some faulty links remaining.
     */
    if (pathname.includes('dashboard') && component && isPortfolioLike(component.qualifier)) {
      router.replace(getPortfolioUrl(component.key));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [component]);

  // Set portal anchor on mount
  React.useEffect(() => {
    portalAnchor.current = document.querySelector('#component-nav-portal');
  }, []);

  const isInProgress = tasksInProgress && tasksInProgress.length > 0;

  const componentProviderProps = React.useMemo(
    () => ({
      component,
      currentTask,
      isInProgress,
      isPending,
      onComponentChange: handleComponentChange,
      fetchComponent,
    }),
    [component, currentTask, isInProgress, isPending, handleComponentChange, fetchComponent],
  );

  // Show not found component when, after loading:
  // - component is not found
  // - target branch is not found (for pull requests fixing issues in a branch)
  if (!loading && (!component || (fixedInPullRequest && !isFetching && !branchLike))) {
    return <ComponentContainerNotFound isPortfolioLike={pathname.includes('portfolio')} />;
  }

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
        portalAnchor.current &&
        /* Use a portal to fix positioning until we can fully review the layout */
        createPortal(
          <ComponentNav
            component={component}
            isInProgress={isInProgress}
            isPending={isPending}
            projectBindingErrors={projectBindingErrors}
          />,
          portalAnchor.current,
        )}
      {loading ? (
        <CenteredLayout>
          <Spinner className="sw-mt-10" />
        </CenteredLayout>
      ) : (
        <ComponentContext.Provider value={componentProviderProps}>
          <Outlet />
        </ComponentContext.Provider>
      )}
    </div>
  );
}

function addQualifier(component: Component) {
  return {
    ...component,
    qualifier: component.breadcrumbs[component.breadcrumbs.length - 1].qualifier,
  };
}

function needsAnotherCheck(
  hasUpdatedTasks: boolean,
  component: Component | undefined,
  newTasksInProgress: Task[],
) {
  return (
    !hasUpdatedTasks && component && (newTasksInProgress.length > 0 || !component.analysisDate)
  );
}

export function isSameBranch(
  task: Pick<Task, 'branch' | 'pullRequest'>,
  branch?: string,
  pullRequest?: string,
) {
  if (!branch?.length && !pullRequest?.length) {
    return !task.branch && !task.pullRequest;
  }

  if (pullRequest?.length) {
    return pullRequest === task.pullRequest;
  }

  return branch === task.branch;
}

function getCurrentTask(
  current?: Task,
  branch?: string,
  pullRequest?: string,
  canBeDifferentBranchLike = false,
) {
  if (!current || !isReportRelatedTask(current)) {
    return undefined;
  }

  return current.status === TaskStatuses.Failed ||
    canBeDifferentBranchLike ||
    isSameBranch(current, branch, pullRequest)
    ? current
    : undefined;
}

function getReportRelatedPendingTasks(
  pendingTasks: Task[],
  branch?: string,
  pullRequest?: string,
  canBeDifferentBranchLike = false,
) {
  return pendingTasks.filter(
    (task) =>
      isReportRelatedTask(task) &&
      (canBeDifferentBranchLike || isSameBranch(task, branch, pullRequest)),
  );
}

function getInProgressTasks(pendingTasks: Task[]) {
  return pendingTasks.filter((task) => task.status === TaskStatuses.InProgress);
}

function isReportRelatedTask(task: Task) {
  return [TaskTypes.AppRefresh, TaskTypes.Report, TaskTypes.ViewRefresh].includes(task.type);
}

function computeHasUpdatedTasks(
  tasksInProgress: Task[] | undefined,
  newTasksInProgress: Task[],
  currentTask: Task | undefined,
  newCurrentTask: Task | undefined,
  component: Component | undefined,
) {
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
      Boolean(!component.analysisDate) || Boolean(component.analysisDate && tasksInProgress?.length)
    );
  }
  return false;
}

export default withAvailableFeatures(ComponentContainer);
