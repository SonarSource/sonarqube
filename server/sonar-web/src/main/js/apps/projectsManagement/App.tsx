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
import Helmet from 'react-helmet';
import { debounce, uniq, without } from 'lodash';
import Header from './Header';
import Search from './Search';
import Projects from './Projects';
import CreateProjectForm from './CreateProjectForm';
import ListFooter from '../../components/controls/ListFooter';
import { PAGE_SIZE, Type, Project } from './utils';
import { getComponents, getProvisioned } from '../../api/components';
import { Organization } from '../../app/types';
import { translate } from '../../helpers/l10n';

export interface Props {
  hasProvisionPermission?: boolean;
  onVisibilityChange: (visibility: string) => void;
  organization: Organization;
  topLevelQualifiers: string[];
}

interface State {
  createProjectForm: boolean;
  page: number;
  projects: Project[];
  qualifiers: string;
  query: string;
  ready: boolean;
  selection: string[];
  total: number;
  type: Type;
}

export default class App extends React.PureComponent<Props, State> {
  mounted: boolean;

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
      type: Type.All,
      selection: []
    };
    this.requestProjects = debounce(this.requestProjects, 250);
  }

  componentDidMount() {
    this.mounted = true;
    this.requestProjects();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getFilters = () => ({
    organization: this.props.organization.key,
    p: this.state.page !== 1 ? this.state.page : undefined,
    ps: PAGE_SIZE,
    q: this.state.query ? this.state.query : undefined
  });

  requestProjects = () => {
    switch (this.state.type) {
      case Type.All:
        this.requestAllProjects();
        break;
      case Type.Provisioned:
        this.requestProvisioned();
        break;
    }
  };

  requestProvisioned = () => {
    const data = this.getFilters();
    getProvisioned(data).then(r => {
      if (this.mounted) {
        let projects: Project[] = r.projects.map((project: any) => ({
          ...project,
          id: project.uuid,
          qualifier: 'TRK'
        }));
        if (this.state.page > 1) {
          projects = [...this.state.projects, ...projects];
        }
        this.setState({ ready: true, projects, selection: [], total: r.paging.total });
      }
    });
  };

  requestAllProjects = () => {
    const data = this.getFilters();
    Object.assign(data, { qualifiers: this.state.qualifiers });
    getComponents(data).then(r => {
      if (this.mounted) {
        let projects: Project[] = r.components;
        if (this.state.page > 1) {
          projects = [...this.state.projects, ...projects];
        }
        this.setState({ ready: true, projects, selection: [], total: r.paging.total });
      }
    });
  };

  loadMore = () => {
    this.setState({ ready: false, page: this.state.page + 1 }, this.requestProjects);
  };

  onSearch = (query: string) => {
    this.setState({ ready: false, page: 1, query, selection: [] }, this.requestProjects);
  };

  onTypeChanged = (newType: Type) => {
    this.setState(
      { ready: false, page: 1, query: '', type: newType, qualifiers: 'TRK', selection: [] },
      this.requestProjects
    );
  };

  onQualifierChanged = (newQualifier: string) => {
    this.setState(
      { ready: false, page: 1, query: '', type: Type.All, qualifiers: newQualifier, selection: [] },
      this.requestProjects
    );
  };

  onProjectSelected = (project: string) => {
    const newSelection = uniq([...this.state.selection, project]);
    this.setState({ selection: newSelection });
  };

  onProjectDeselected = (project: string) => {
    const newSelection = without(this.state.selection, project);
    this.setState({ selection: newSelection });
  };

  onAllSelected = () => {
    const newSelection = this.state.projects.map(project => project.key);
    this.setState({ selection: newSelection });
  };

  onAllDeselected = () => {
    this.setState({ selection: [] });
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
          onAllSelected={this.onAllSelected}
          onAllDeselected={this.onAllDeselected}
          onDeleteProjects={this.requestProjects}
          onQualifierChanged={this.onQualifierChanged}
          onSearch={this.onSearch}
          onTypeChanged={this.onTypeChanged}
        />

        <Projects
          ready={this.state.ready}
          projects={this.state.projects}
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
            organization={this.props.organization}
          />}
      </div>
    );
  }
}
