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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import { debounce, uniq, without } from 'lodash';
import Header from './header';
import Search from './search';
import Projects from './projects';
import CreateProjectForm from './CreateProjectForm';
import ListFooter from '../../components/controls/ListFooter';
import { PAGE_SIZE, TYPE } from './constants';
import { getComponents, getProvisioned, getGhosts, deleteComponents } from '../../api/components';
import { translate } from '../../helpers/l10n';
import type { Organization } from '../../store/organizations/duck';

type Props = {|
  hasProvisionPermission: boolean,
  onVisibilityChange: string => void,
  onRequestFail: Object => void,
  organization: Organization
|};

type State = {
  createProjectForm: boolean,
  ready: boolean,
  projects: Array<{ key: string }>,
  total: number,
  page: number,
  query: string,
  qualifiers: string,
  type: string,
  selection: Array<string>
};

export default class Main extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      createProjectForm: false,
      ready: false,
      projects: [],
      total: 0,
      page: 1,
      query: '',
      qualifiers: 'TRK',
      type: TYPE.ALL,
      selection: []
    };
    this.requestProjects = debounce(this.requestProjects, 250);
  }

  componentDidMount() {
    this.requestProjects();
  }

  getFilters = () => {
    const filters: { [string]: string | number } = {
      organization: this.props.organization.key,
      ps: PAGE_SIZE
    };
    if (this.state.page !== 1) {
      filters.p = this.state.page;
    }
    if (this.state.query) {
      filters.q = this.state.query;
    }
    return filters;
  };

  requestProjects = () => {
    switch (this.state.type) {
      case TYPE.ALL:
        this.requestAllProjects();
        break;
      case TYPE.PROVISIONED:
        this.requestProvisioned();
        break;
      case TYPE.GHOSTS:
        this.requestGhosts();
        break;
      default:

      // should never happen
    }
  };

  requestGhosts = () => {
    const data = this.getFilters();
    getGhosts(data).then(r => {
      let projects = r.projects.map(project => ({
        ...project,
        id: project.uuid,
        qualifier: 'TRK'
      }));
      if (this.state.page > 1) {
        projects = [].concat(this.state.projects, projects);
      }
      this.setState({ ready: true, projects, total: r.total });
    });
  };

  requestProvisioned = () => {
    const data = this.getFilters();
    getProvisioned(data).then(r => {
      let projects = r.projects.map(project => ({
        ...project,
        id: project.uuid,
        qualifier: 'TRK'
      }));
      if (this.state.page > 1) {
        projects = [].concat(this.state.projects, projects);
      }
      this.setState({ ready: true, projects, total: r.paging.total });
    });
  };

  requestAllProjects = () => {
    const data = this.getFilters();
    data.qualifiers = this.state.qualifiers;
    getComponents(data).then(r => {
      let projects = r.components;
      if (this.state.page > 1) {
        projects = [].concat(this.state.projects, projects);
      }
      this.setState({ ready: true, projects, total: r.paging.total });
    });
  };

  loadMore = () => {
    this.setState({ ready: false, page: this.state.page + 1 }, this.requestProjects);
  };

  onSearch = (query: string) => {
    this.setState(
      {
        ready: false,
        page: 1,
        query,
        selection: []
      },
      this.requestProjects
    );
  };

  onTypeChanged = (newType: string) => {
    this.setState(
      {
        ready: false,
        page: 1,
        query: '',
        type: newType,
        qualifiers: 'TRK',
        selection: []
      },
      this.requestProjects
    );
  };

  onQualifierChanged = (newQualifier: string) => {
    this.setState(
      {
        ready: false,
        page: 1,
        query: '',
        type: TYPE.ALL,
        qualifiers: newQualifier,
        selection: []
      },
      this.requestProjects
    );
  };

  onProjectSelected = (project: { key: string }) => {
    const newSelection = uniq([].concat(this.state.selection, project.key));
    this.setState({ selection: newSelection });
  };

  onProjectDeselected = (project: { key: string }) => {
    const newSelection = without(this.state.selection, project.key);
    this.setState({ selection: newSelection });
  };

  onAllSelected = () => {
    const newSelection = this.state.projects.map(project => project.key);
    this.setState({ selection: newSelection });
  };

  onAllDeselected = () => {
    this.setState({ selection: [] });
  };

  deleteProjects = () => {
    this.setState({ ready: false });
    const projects = this.state.selection.join(',');
    const data = {
      organization: this.props.organization.key,
      projects
    };
    deleteComponents(data).then(() => {
      this.setState({ page: 1, selection: [] }, this.requestProjects);
    });
  };

  openCreateProjectForm = () => {
    this.setState({ createProjectForm: true });
  };

  closeCreateProjectForm = () => {
    this.setState({ createProjectForm: false });
  };

  render() {
    return (
      <div className="page page-limited" id="projects-management-page">
        <Helmet title={translate('projects_management')} />

        <Header
          hasProvisionPermission={this.props.hasProvisionPermission}
          onProjectCreate={this.openCreateProjectForm}
          onVisibilityChange={this.props.onVisibilityChange}
          organization={this.props.organization}
        />

        <Search
          {...this.props}
          {...this.state}
          onSearch={this.onSearch}
          onTypeChanged={this.onTypeChanged}
          onQualifierChanged={this.onQualifierChanged}
          onAllSelected={this.onAllSelected}
          onAllDeselected={this.onAllDeselected}
          deleteProjects={this.deleteProjects}
        />

        <Projects
          ready={this.state.ready}
          projects={this.state.projects}
          refresh={this.requestProjects}
          selection={this.state.selection}
          onProjectSelected={this.onProjectSelected}
          onProjectDeselected={this.onProjectDeselected}
          organization={this.props.organization}
        />

        <ListFooter
          ready={this.state.ready}
          count={this.state.projects.length}
          total={this.state.total}
          loadMore={this.loadMore}
        />

        {this.state.createProjectForm &&
          <CreateProjectForm
            onClose={this.closeCreateProjectForm}
            onProjectCreated={this.requestProjects}
            onRequestFail={this.props.onRequestFail}
            organization={this.props.organization}
          />}
      </div>
    );
  }
}
