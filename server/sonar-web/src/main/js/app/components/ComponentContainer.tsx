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
import { Branch, Component } from '../types';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';
import { getBranches } from '../../api/branches';
import { Task, getTasksForComponent } from '../../api/ce';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/nav';
import { fetchOrganizations } from '../../store/rootActions';
import { STATUSES } from '../../apps/background-tasks/constants';

interface Props {
  children: any;
  fetchOrganizations: (organizations: string[]) => void;
  location: {
    query: { branch?: string; id: string };
  };
}

interface State {
  branches: Branch[];
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
    this.state = { branches: [], loading: true };
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
    const { branch, id } = props.location.query;
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

    Promise.all([getComponentNavigation(id, branch), getComponentData(id, branch)]).then(
      ([nav, data]) => {
        const component = this.addQualifier({ ...nav, ...data });

        if (this.context.organizationsEnabled) {
          this.props.fetchOrganizations([component.organization]);
        }

        this.fetchBranches(component).then(branches => {
          if (this.mounted) {
            this.setState({ loading: false, branches, component });
          }
        }, onError);

        this.fetchStatus(component);
      },
      onError
    );
  }

  fetchBranches = (component: Component) => {
    const project = component.breadcrumbs.find(({ qualifier }) => qualifier === 'TRK');
    return project ? getBranches(project.key) : Promise.resolve([]);
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
        branches => {
          if (this.mounted) {
            this.setState({ branches });
          }
        },
        () => {}
      );
    }
  };

  render() {
    const { query } = this.props.location;
    const { branches, component, loading } = this.state;

    if (!loading && !component) {
      return <ComponentContainerNotFound />;
    }

    const branch = branches.find(b => (query.branch ? b.name === query.branch : b.isMain));

    return (
      <div>
        {component &&
          !['FIL', 'UTS'].includes(component.qualifier) && (
            <ComponentNav
              branches={branches}
              currentBranch={branch}
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
            branch,
            branches,
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
