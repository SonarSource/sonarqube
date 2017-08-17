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
import ComponentNav from './nav/component/ComponentNav';
import { Branch, Component } from '../types';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';
import { getBranch } from '../../api/branches';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/nav';
import { MAIN_BRANCH } from '../../helpers/branches';

interface Props {
  children: any;
  location: {
    query: { branch?: string; id: string };
  };
}

interface State {
  branch: Branch | null;
  loading: boolean;
  component: Component | null;
}

export default class ProjectContainer extends React.PureComponent<Props, State> {
  mounted: boolean;

  constructor(props: Props) {
    super(props);
    this.state = { branch: null, loading: true, component: null };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchProject();
  }

  componentWillReceiveProps(nextProps: Props) {
    // if the current branch has been changed, reset `branch` in state
    // it prevents unwanted redirect in `overview/App#componentDidMount`
    if (nextProps.location.query.branch !== this.props.location.query.branch) {
      this.setState({ branch: null });
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.location.query.id !== this.props.location.query.id ||
      prevProps.location.query.branch !== this.props.location.query.branch
    ) {
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
    Promise.all([
      getComponentNavigation(id),
      getComponentData(id, branch),
      branch && getBranch(id, branch)
    ]).then(
      ([nav, data, branch]) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            branch: branch || MAIN_BRANCH,
            component: this.addQualifier({ ...nav, ...data })
          });
        }
      },
      error => {
        if (this.mounted) {
          if (error.response && error.response.status === 403) {
            handleRequiredAuthorization();
          } else {
            this.setState({ loading: false });
          }
        }
      }
    );
  }

  handleProjectChange = (changes: {}) => {
    if (this.mounted) {
      this.setState(state => ({ project: { ...state.component, ...changes } }));
    }
  };

  render() {
    const { branch, component } = this.state;

    if (!component || !branch) {
      return null;
    }

    const isFile = ['FIL', 'UTS'].includes(component.qualifier);
    const configuration = component.configuration || {};

    return (
      <div>
        {!isFile &&
          <ComponentNav
            branch={branch}
            component={component}
            conf={configuration}
            location={this.props.location}
          />}
        {React.cloneElement(this.props.children, {
          branch,
          component: component,
          onComponentChange: this.handleProjectChange
        })}
      </div>
    );
  }
}
