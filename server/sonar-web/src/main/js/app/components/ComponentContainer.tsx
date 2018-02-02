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
import ComponentContainerNotFound from './ComponentContainerNotFound';
import ComponentNav from './nav/component/ComponentNav';
import { Component, BranchLike } from '../types';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';
import { getBranches, getPullRequests } from '../../api/branches';
import { Task, getTasksForComponent } from '../../api/ce';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/nav';
import { fetchOrganizations } from '../../store/rootActions';
import { STATUSES } from '../../apps/background-tasks/constants';
import { isPullRequest, isBranch } from '../../helpers/branches';

interface Props {
  children: any;
  fetchOrganizations: (organizations: string[]) => void;
  location: {
    query: { branch?: string; id: string; pullRequest?: string };
  };
}

interface State {
  branchLikes: BranchLike[];
  loading: boolean;
  component?: Component;
  currentTask?: Task;
  isInProgress?: boolean;
  isPending?: boolean;
}

export class ComponentContainer extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    organizationsEnabled: PropTypes.bool
  };

  constructor(props: Props) {
    super(props);
    this.state = { branchLikes: [], loading: true };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent(this.props);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      nextProps.location.query.id !== this.props.location.query.id ||
      nextProps.location.query.branch !== this.props.location.query.branch
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

  fetchComponent(props: Props) {
    const { branch, id: key } = props.location.query;
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
      getComponentNavigation({ componentKey: key, branch }),
      getComponentData({ component: key, branch })
    ]).then(([nav, data]) => {
      const component = this.addQualifier({ ...nav, ...data });

      if (this.context.organizationsEnabled) {
        this.props.fetchOrganizations([component.organization]);
      }

      this.fetchBranches(component).then(branchLikes => {
        if (this.mounted) {
          this.setState({ loading: false, branchLikes, component });
        }
      }, onError);

      this.fetchStatus(component);
    }, onError);
  }

  fetchBranches = (component: Component): Promise<BranchLike[]> => {
    const project = component.breadcrumbs.find(({ qualifier }) => qualifier === 'TRK');
    return project
      ? Promise.all([getBranches(project.key), getPullRequests(project.key)]).then(
          ([branches, pullRequests]) => [...branches, ...pullRequests]
        )
      : Promise.resolve([]);
  };

  fetchStatus = (component: Component) => {
    getTasksForComponent(component.key).then(
      ({ current, queue }) => {
        if (this.mounted) {
          this.setState({
            currentTask: current,
            isInProgress: queue.some(task => task.status === STATUSES.IN_PROGRESS),
            isPending: queue.some(task => task.status === STATUSES.PENDING)
          });
        }
      },
      () => {}
    );
  };

  handleComponentChange = (changes: {}) => {
    if (this.mounted) {
      this.setState(state => ({ component: { ...state.component, ...changes } }));
    }
  };

  handleBranchesChange = () => {
    if (this.mounted && this.state.component) {
      this.fetchBranches(this.state.component).then(
        branchLikes => {
          if (this.mounted) {
            this.setState({ branchLikes });
          }
        },
        () => {}
      );
    }
  };

  render() {
    const { query } = this.props.location;
    const { branchLikes, component, loading } = this.state;

    if (!loading && !component) {
      return <ComponentContainerNotFound />;
    }

    const branchLike = query.pullRequest
      ? branchLikes.find(b => isPullRequest(b) && b.id === query.pullRequest)
      : branchLikes.find(b => isBranch(b) && (query.branch ? b.name === query.branch : b.isMain));

    return (
      <div>
        {component &&
          !['FIL', 'UTS'].includes(component.qualifier) && (
            <ComponentNav
              branchLikes={branchLikes}
              currentBranchLike={branchLike}
              component={component}
              currentTask={this.state.currentTask}
              isInProgress={this.state.isInProgress}
              isPending={this.state.isPending}
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
            isInProgress: this.state.isInProgress,
            isPending: this.state.isPending,
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
