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
import Helmet from 'react-helmet';
import { debounce, uniq, without } from 'lodash';
import Header from './Header';
import Search from './Search';
import Projects from './Projects';
import CreateProjectForm from './CreateProjectForm';
import { PAGE_SIZE, Project } from './utils';
import ListFooter from '../../components/controls/ListFooter';
import { getComponents } from '../../api/components';
import { Organization } from '../../app/types';
import { translate } from '../../helpers/l10n';

export interface Props {
  currentUser: { login: string };
  hasProvisionPermission?: boolean;
  onVisibilityChange: (visibility: string) => void;
  organization: Organization;
  topLevelQualifiers: string[];
}

interface State {
  analyzedBefore?: string;
  createProjectForm: boolean;
  page: number;
  projects: Project[];
  provisioned: boolean;
  qualifiers: string;
  query: string;
  ready: boolean;
  selection: string[];
  total: number;
}

export default class App extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      createProjectForm: false,
      ready: false,
      projects: [],
      provisioned: false,
      total: 0,
      page: 1,
      query: '',
      qualifiers: 'TRK',
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

  requestProjects = () => {
    const parameters = {
      analyzedBefore: this.state.analyzedBefore,
      onProvisionedOnly: this.state.provisioned || undefined,
      organization: this.props.organization.key,
      p: this.state.page !== 1 ? this.state.page : undefined,
      ps: PAGE_SIZE,
      q: this.state.query || undefined,
      qualifiers: this.state.qualifiers
    };
    getComponents(parameters).then(r => {
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

  onProvisionedChanged = (provisioned: boolean) => {
    this.setState(
      { ready: false, page: 1, query: '', provisioned, qualifiers: 'TRK', selection: [] },
      this.requestProjects
    );
  };

  onQualifierChanged = (newQualifier: string) => {
    this.setState(
      {
        ready: false,
        page: 1,
        provisioned: false,
        query: '',
        qualifiers: newQualifier,
        selection: []
      },
      this.requestProjects
    );
  };

  handleDateChanged = (analyzedBefore?: string) =>
    this.setState({ ready: false, page: 1, analyzedBefore }, this.requestProjects);

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
          analyzedBefore={this.state.analyzedBefore}
          onAllSelected={this.onAllSelected}
          onAllDeselected={this.onAllDeselected}
          onDateChanged={this.handleDateChanged}
          onDeleteProjects={this.requestProjects}
          onProvisionedChanged={this.onProvisionedChanged}
          onQualifierChanged={this.onQualifierChanged}
          onSearch={this.onSearch}
          organization={this.props.organization}
          projects={this.state.projects}
          provisioned={this.state.provisioned}
          qualifiers={this.state.qualifiers}
          query={this.state.query}
          ready={this.state.ready}
          selection={this.state.selection}
          topLevelQualifiers={this.props.topLevelQualifiers}
          total={this.state.total}
        />

        <Projects
          currentUser={this.props.currentUser}
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

        {this.state.createProjectForm && (
          <CreateProjectForm
            onClose={this.closeCreateProjectForm}
            onProjectCreated={this.requestProjects}
            organization={this.props.organization}
          />
        )}
      </div>
    );
  }
}
