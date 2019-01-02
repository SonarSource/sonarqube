/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Suggestions from '../../app/components/embed-docs-modal/Suggestions';
import { getComponents, Project } from '../../api/components';
import { toNotSoISOString } from '../../helpers/dates';
import { translate } from '../../helpers/l10n';

export interface Props {
  currentUser: { login: string };
  hasProvisionPermission?: boolean;
  onOrganizationUpgrade: () => void;
  onVisibilityChange: (visibility: T.Visibility) => void;
  organization: T.Organization;
  topLevelQualifiers: string[];
}

interface State {
  analyzedBefore?: Date;
  createProjectForm: boolean;
  page: number;
  projects: Project[];
  provisioned: boolean;
  qualifiers: string;
  query: string;
  ready: boolean;
  selection: string[];
  total: number;
  visibility?: T.Visibility;
}

const PAGE_SIZE = 50;

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
    const { analyzedBefore } = this.state;
    const parameters = {
      analyzedBefore: analyzedBefore && toNotSoISOString(analyzedBefore),
      onProvisionedOnly: this.state.provisioned || undefined,
      organization: this.props.organization.key,
      p: this.state.page !== 1 ? this.state.page : undefined,
      ps: PAGE_SIZE,
      q: this.state.query || undefined,
      qualifiers: this.state.qualifiers,
      visibility: this.state.visibility
    };
    getComponents(parameters).then(
      r => {
        if (this.mounted) {
          let projects: Project[] = r.components;
          if (this.state.page > 1) {
            projects = [...this.state.projects, ...projects];
          }
          this.setState({ ready: true, projects, selection: [], total: r.paging.total });
        }
      },
      () => {}
    );
  };

  loadMore = () => {
    this.setState(({ page }) => ({ ready: false, page: page + 1 }), this.requestProjects);
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

  onVisibilityChanged = (newVisibility: T.Visibility | 'all') => {
    this.setState(
      {
        ready: false,
        page: 1,
        provisioned: false,
        query: '',
        visibility: newVisibility === 'all' ? undefined : newVisibility,
        selection: []
      },
      this.requestProjects
    );
  };

  handleDateChanged = (analyzedBefore: Date | undefined) =>
    this.setState({ ready: false, page: 1, analyzedBefore }, this.requestProjects);

  onProjectSelected = (project: string) => {
    this.setState(({ selection }) => ({ selection: uniq([...selection, project]) }));
  };

  onProjectDeselected = (project: string) => {
    this.setState(({ selection }) => ({ selection: without(selection, project) }));
  };

  onAllSelected = () => {
    this.setState(({ projects }) => ({ selection: projects.map(project => project.key) }));
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
        <Suggestions suggestions="projects_management" />
        <Helmet title={translate('projects_management')} />

        <Header
          hasProvisionPermission={this.props.hasProvisionPermission}
          onProjectCreate={this.openCreateProjectForm}
          onVisibilityChange={this.props.onVisibilityChange}
          organization={this.props.organization}
        />

        <Search
          analyzedBefore={this.state.analyzedBefore}
          onAllDeselected={this.onAllDeselected}
          onAllSelected={this.onAllSelected}
          onDateChanged={this.handleDateChanged}
          onDeleteProjects={this.requestProjects}
          onProvisionedChanged={this.onProvisionedChanged}
          onQualifierChanged={this.onQualifierChanged}
          onSearch={this.onSearch}
          onVisibilityChanged={this.onVisibilityChanged}
          organization={this.props.organization}
          projects={this.state.projects}
          provisioned={this.state.provisioned}
          qualifiers={this.state.qualifiers}
          query={this.state.query}
          ready={this.state.ready}
          selection={this.state.selection}
          topLevelQualifiers={this.props.topLevelQualifiers}
          total={this.state.total}
          visibility={this.state.visibility}
        />

        <Projects
          currentUser={this.props.currentUser}
          onProjectDeselected={this.onProjectDeselected}
          onProjectSelected={this.onProjectSelected}
          organization={this.props.organization}
          projects={this.state.projects}
          ready={this.state.ready}
          selection={this.state.selection}
        />

        <ListFooter
          count={this.state.projects.length}
          loadMore={this.loadMore}
          ready={this.state.ready}
          total={this.state.total}
        />

        {this.state.createProjectForm && (
          <CreateProjectForm
            onClose={this.closeCreateProjectForm}
            onOrganizationUpgrade={this.props.onOrganizationUpgrade}
            onProjectCreated={this.requestProjects}
            organization={this.props.organization}
          />
        )}
      </div>
    );
  }
}
