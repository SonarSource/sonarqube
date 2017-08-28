/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import ProjectContainerNotFound from './ProjectContainerNotFound';
import ComponentNav from './nav/component/ComponentNav';
import { Branch, Component } from '../types';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';
import { getBranches } from '../../api/branches';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/nav';

interface Props {
  children: any;
  location: {
    query: { branch?: string; id: string };
  };
}

interface State {
  branches: Branch[];
  loading: boolean;
  component: Component | null;
}

export default class ProjectContainer extends React.PureComponent<Props, State> {
  mounted: boolean;

  constructor(props: Props) {
    super(props);
    this.state = { branches: [], loading: true, component: null };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchProject();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query.id !== this.props.location.query.id) {
      this.fetchProject();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  addQualifier = (component: Component) => ({
    ...component,
    qualifier: component.breadcrumbs[component.breadcrumbs.length - 1].qualifier
  });

  fetchProject() {
    const { branch, id } = this.props.location.query;
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

    Promise.all([getComponentNavigation(id), getComponentData(id, branch)]).then(([nav, data]) => {
      const component = this.addQualifier({ ...nav, ...data });
      this.fetchBranches(component).then(branches => {
        if (this.mounted) {
          this.setState({ loading: false, branches, component });
        }
      }, onError);
    }, onError);
  }

  fetchBranches = (component: Component) => {
    const project = component.breadcrumbs.find((c: Component) => c.qualifier === 'TRK');
    return project ? getBranches(project.key) : Promise.resolve([]);
  };

  handleProjectChange = (changes: {}) => {
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

    if (loading) {
      return <i className="spinner" />;
    }

    const branch = branches.find(b => (query.branch ? b.name === query.branch : b.isMain));

    if (!component || !branch) {
      return <ProjectContainerNotFound />;
    }

    const isFile = ['FIL', 'UTS'].includes(component.qualifier);
    const configuration = component.configuration || {};

    return (
      <div>
        {!isFile &&
          <ComponentNav
            branches={branches}
            currentBranch={branch}
            component={component}
            conf={configuration}
            location={this.props.location}
          />}
        {React.cloneElement(this.props.children, {
          branch,
          branches,
          component: component,
          onBranchesChange: this.handleBranchesChange,
          onComponentChange: this.handleProjectChange
        })}
      </div>
    );
  }
}
