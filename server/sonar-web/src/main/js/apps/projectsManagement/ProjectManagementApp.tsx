/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { debounce, uniq, without } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { getComponents, Project } from '../../api/components';
import { changeProjectDefaultVisibility } from '../../api/permissions';
import { getValue } from '../../api/settings';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import ListFooter from '../../components/controls/ListFooter';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { toShortNotSoISOString } from '../../helpers/dates';
import { throwGlobalError } from '../../helpers/error';
import { translate } from '../../helpers/l10n';
import { hasGlobalPermission } from '../../helpers/users';
import { Permissions } from '../../types/permissions';
import { SettingsKey } from '../../types/settings';
import { Organization, Visibility } from '../../types/types';
import { LoggedInUser } from '../../types/users';
import CreateProjectForm from './CreateProjectForm';
import Header from './Header';
import Projects from './Projects';
import Search from './Search';
import { withOrganizationContext } from "../organizations/OrganizationContext";

export interface Props {
  currentUser: LoggedInUser;
  organization: Organization;
}

interface State {
  analyzedBefore?: Date;
  createProjectForm: boolean;
  defaultProjectVisibility?: Visibility;
  page: number;
  projects: Project[];
  provisioned: boolean;
  qualifiers: string;
  query: string;
  ready: boolean;
  selection: string[];
  total: number;
  visibility?: Visibility;
}

const DEBOUNCE_DELAY = 250;
const PAGE_SIZE = 50;

export class ProjectManagementApp extends React.PureComponent<Props, State> {
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
      selection: [],
    };
    this.requestProjects = debounce(this.requestProjects, DEBOUNCE_DELAY);
  }

  componentDidMount() {
    this.mounted = true;
    this.requestProjects();
    this.fetchDefaultProjectVisibility();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchDefaultProjectVisibility = async () => {
    const results = await getValue({ key: SettingsKey.DefaultProjectVisibility });

    if (this.mounted && results?.value !== undefined) {
      this.setState({ defaultProjectVisibility: results.value as Visibility });
    }
  };

  handleDefaultProjectVisibilityChange = async (visibility: Visibility) => {
    await changeProjectDefaultVisibility(this.props.organization.kee, visibility);

    if (this.mounted) {
      this.setState({ defaultProjectVisibility: visibility });
    }
  };

  requestProjects = () => {
    const { analyzedBefore } = this.state;
    const parameters = {
      analyzedBefore: analyzedBefore && toShortNotSoISOString(analyzedBefore),
      onProvisionedOnly: this.state.provisioned || undefined,
      organization: this.props.organization.kee,
      p: this.state.page !== 1 ? this.state.page : undefined,
      ps: PAGE_SIZE,
      q: this.state.query || undefined,
      qualifiers: this.state.qualifiers,
      visibility: this.state.visibility,
    };
    getComponents(parameters)
      .then((r) => {
        if (this.mounted) {
          let projects: Project[] = r.components;
          if (this.state.page > 1) {
            projects = [...this.state.projects, ...projects];
          }
          this.setState({ ready: true, projects, selection: [], total: r.paging.total });
        }
      })
      .catch(throwGlobalError);
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
        selection: [],
      },
      this.requestProjects
    );
  };

  onVisibilityChanged = (newVisibility: Visibility | 'all') => {
    this.setState(
      {
        ready: false,
        page: 1,
        provisioned: false,
        query: '',
        visibility: newVisibility === 'all' ? undefined : newVisibility,
        selection: [],
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
    this.setState(({ projects }) => ({ selection: projects.map((project) => project.key) }));
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
    const { currentUser } = this.props;
    const { defaultProjectVisibility } = this.state;
    return (
      <div className="page page-limited" id="projects-management-page">
        <Suggestions suggestions="projects_management" />
        <Helmet defer={false} title={translate('projects_management')} />

        <Header
          defaultProjectVisibility={defaultProjectVisibility}
          hasProvisionPermission={hasGlobalPermission(currentUser, Permissions.ProjectCreation)}
          onChangeDefaultProjectVisibility={this.handleDefaultProjectVisibilityChange}
          onProjectCreate={this.openCreateProjectForm}
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
          projects={this.state.projects}
          provisioned={this.state.provisioned}
          qualifiers={this.state.qualifiers}
          query={this.state.query}
          ready={this.state.ready}
          selection={this.state.selection}
          total={this.state.total}
          visibility={this.state.visibility}
          organization={this.props.organization.kee}
        />

        <Projects
          currentUser={this.props.currentUser}
          onProjectDeselected={this.onProjectDeselected}
          onProjectSelected={this.onProjectSelected}
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
            defaultProjectVisibility={defaultProjectVisibility}
            onClose={this.closeCreateProjectForm}
            onProjectCreated={this.requestProjects}
          />
        )}
      </div>
    );
  }
}

export default withCurrentUserContext(withOrganizationContext(ProjectManagementApp));
