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

import {
  getBranchLikeQuery,
  isBranch,
  isMainBranch,
  isPullRequest,
} from '~sonar-aligned/helpers/branch-like';
import { withOrganizationContext } from "../../organizations/OrganizationContext";
import withCurrentUserContext from "../../../app/components/current-user/withCurrentUserContext";
import { LoggedInUser } from '../../../types/users';
import { Component, Organization, Status } from '../../../types/types';
import { searchProjects } from '../../../api/components';

import { getComponentData } from '../../../api/components';
import { getComponentNavigation } from '../../../api/navigation';

import { getOrganization, getOrganizationNavigation } from "../../../api/organizations";
import { HttpStatus } from '../../../helpers/request';
import handleRequiredAuthorization from '../../../../js/app/utils/handleRequiredAuthorization';
import { BranchLike } from '../../../types/branch-like';
import { Feature } from '../../../types/features';
import { getBranches } from '../../../api/branches';
import { getPullRequests } from '../../../api/branches';
import { getAnalysisStatus, getTasksForComponent } from '../../../api/ce';
import { Router } from '../../../../js/components/hoc/withRouter';
import withAvailableFeatures, {
  WithAvailableFeaturesProps
} from '../../../../js/app/components/available-features/withAvailableFeatures';
import { Task, TaskStatuses, TaskTypes } from '../../../types/tasks';
import { differenceBy } from 'lodash';
import BranchOverview from '../../overview/branches/BranchOverview';
import { Spinner } from "@sonarsource/echoes-react";
import { ComponentQualifier } from "~sonar-aligned/types/component";
import withBranchStatusActions from '../../../app/components/branch-status/withBranchStatusActions';

const FETCH_STATUS_WAIT_TIME = 3000;

interface Props extends WithAvailableFeaturesProps {
  location: Location;
  updateBranchStatus: (branchLike: BranchLike, component: string, status: Status) => void;
  router: Router;
  currentUser: LoggedInUser;
  organization: Organization;
}

interface State {
  projects: any[],
  loadingProjects: boolean,
  selectedOption: string,
  loading: boolean,
  branchLikes: any[],
  comparisonBranchesEnabled: false,
  isPending: false,
  warnings: any[],
  branchLike: any,
  component: any
  organization: any,
  currentTask: any,
  tasksInProgress: any[]
}

class PolicyResults extends React.PureComponent<Props, State> {
  mounted = false;
  orgId: string = '';
  watchStatusTimer: any;


  componentDidMount() {
    this.mounted = true;
    const { currentUser, organization } = { ...this.props }
    this.orgId = organization.kee
    this.setState({ loadingProjects: true })
    this.setState({ projects: [] })
    searchProjects({ filter: 'tags=policy', organization: this.orgId }).then((res) => {
      this.setState({ projects: res.components })
      if (res.components.length > 0) {
        const key = res.components[0].key;
        this.setState({ selectedOption: key })
        this.fetchComponent(key);
      }
      this.setState({ loadingProjects: false })
    })
  }

  componentWillUnmount() {
    this.mounted = false;
  }


  handleChange = ({ target }: any) => {

    this.setState({ selectedOption: target.value });
    this.fetchComponent(target.value);
  }

  fetchComponent = async (id: string) => {
    const branch = undefined;
    const pullRequest = undefined;
    const key = id;
    this.setState({ loading: true });

    let componentWithQualifier;
    try {
      const [nav, { component }] = await Promise.all([
        getComponentNavigation({ component: key, branch, pullRequest }),
        getComponentData({ component: key, branch, pullRequest }),
      ]);
      componentWithQualifier = this.addQualifier({ ...nav, ...component });

      const [organization, navigation] = await Promise.all([
        getOrganization(component.organization),
        getOrganizationNavigation(component.organization)
      ]);
      this.setState({ organization: { ...organization, ...navigation } });
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

    const { branchLike, branchLikes } = await this.fetchBranches(componentWithQualifier);

    if (this.mounted) {
      this.setState({
        branchLike,
        branchLikes,
        component: componentWithQualifier,
        loading: false,
      });

      this.fetchStatus(componentWithQualifier.key);
      this.fetchWarnings(componentWithQualifier, branchLike);
    }
  }

  addQualifier = (component: Component) => ({
    ...component,
    qualifier: component.breadcrumbs[component.breadcrumbs.length - 1].qualifier,
  });

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
      // branchLike = this.getCurrentBranchLike(branchLikes);

      this.registerBranchStatuses(branchLikes, componentWithQualifier);
    }

    return { branchLike, branchLikes };
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

  isReportRelatedTask = (task: Task) => {
    return [TaskTypes.AppRefresh, TaskTypes.Report, TaskTypes.ViewRefresh].includes(task.type);
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
                const selectedOption = this.state.selectedOption
                this.fetchComponent(selectedOption);
              }
            }
          );
        }
      },
      () => {
      }
    );
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

  fetchWarnings = (component: Component, branchLike?: BranchLike) => {
    if (component.qualifier === ComponentQualifier.Project) {
      getAnalysisStatus({
        component: component.key,
        ...getBranchLikeQuery(branchLike),
      }).then(
        ({ component }) => {
          this.setState({ warnings: component.warnings });
        },
        () => {
        }
      );
    }
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

  render() {
    const { loading, component, branchLike, loadingProjects, projects, selectedOption } = { ...this.state }
    const branchSupportEnabled = false;
    const projectBinding: any = {};
    const headerStyle = {
      boxSizing: 'border-box',
      margin: '0px auto',
      maxWidth: '1280px',
      paddingLeft: '3.75rem',
      marginBottom: '1.75rem',
    };

    return (
      <div className="page page-limited" style={{ paddingBottom: "0" }}>
        <header className='page-header' style={headerStyle}>
          <h1>Policy Results</h1>
        </header>
        <Spinner className="sw-my-2" isLoading={loading || loadingProjects}>
          <div className="display-flex-row" style={headerStyle}>
            <div className="width-25 big-spacer-right">
              {
                projects?.length == 0 ? (<>
                  <span> No projects found in the organization with "policy" tag </span> </>) : (<>
                  <span>Select Project: </span>
                  <br/>
                  <select style={{ maxWidth: "100%" }}
                          value={selectedOption}
                          onChange={this.handleChange}>
                    {projects?.map(({ key, name }, index) => <option key={key} value={key}>{name}</option>)}
                  </select>
                </>)
              }
            </div>
          </div>
          {projects?.length > 0 && component?.analysisDate && (
            <div>
              <BranchOverview
                branch={branchLike}
                branchesEnabled={branchSupportEnabled}
                component={component}
                projectBinding={projectBinding}
                grc={true}
              />
            </div>
          )}
        </Spinner>
      </div>
    )
  }
}

export default withCurrentUserContext(withOrganizationContext(withBranchStatusActions(withAvailableFeatures(PolicyResults))));