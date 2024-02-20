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
import { LargeCenteredLayout, PageContentFontWrapper } from 'design-system';
import { debounce, uniq } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import {
  Project,
  changeProjectDefaultVisibility,
  getComponents,
} from '../../api/project-management';
import { getValue } from '../../api/settings';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import ListFooter from '../../components/controls/ListFooter';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { toShortISO8601String } from '../../helpers/dates';
import { throwGlobalError } from '../../helpers/error';
import { translate } from '../../helpers/l10n';
import { hasGlobalPermission } from '../../helpers/users';
import { Visibility } from '../../types/component';
import { Permissions } from '../../types/permissions';
import { SettingsKey } from '../../types/settings';
import { LoggedInUser } from '../../types/users';
import Header from './Header';
import Projects from './Projects';
import Search from './Search';

export interface Props {
  currentUser: LoggedInUser;
}

interface State {
  analyzedBefore?: Date;
  defaultProjectVisibility?: Visibility;
  page: number;
  projects: Project[];
  provisioned: boolean;
  qualifiers: string;
  query: string;
  ready: boolean;
  selection: Project[];
  total: number;
  visibility?: Visibility;
}

const DEBOUNCE_DELAY = 250;
const PAGE_SIZE = 50;

class ProjectManagementApp extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
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
    await changeProjectDefaultVisibility(visibility);

    if (this.mounted) {
      this.setState({ defaultProjectVisibility: visibility });
    }
  };

  requestProjects = () => {
    const { analyzedBefore } = this.state;
    const parameters = {
      analyzedBefore: analyzedBefore && toShortISO8601String(analyzedBefore),
      onProvisionedOnly: this.state.provisioned || undefined,
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
      this.requestProjects,
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
      this.requestProjects,
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
      this.requestProjects,
    );
  };

  handleDateChanged = (analyzedBefore: Date | undefined) =>
    this.setState({ ready: false, page: 1, analyzedBefore }, this.requestProjects);

  onProjectSelected = (project: Project) => {
    this.setState(({ selection }) => ({
      selection: uniq([...selection, project]),
    }));
  };

  onProjectDeselected = (project: Project) => {
    this.setState(({ selection }) => ({
      selection: selection.filter(({ key }) => key !== project.key),
    }));
  };

  onAllSelected = () => {
    this.setState(({ projects }) => ({
      selection: projects,
    }));
  };

  onAllDeselected = () => {
    this.setState({ selection: [] });
  };

  render() {
    const { currentUser } = this.props;
    const { defaultProjectVisibility } = this.state;
    return (
      <LargeCenteredLayout as="main" id="projects-management-page">
        <PageContentFontWrapper className="sw-body-sm sw-my-8">
          <Suggestions suggestions="projects_management" />
          <Helmet defer={false} title={translate('projects_management')} />

          <Header
            defaultProjectVisibility={defaultProjectVisibility}
            hasProvisionPermission={hasGlobalPermission(currentUser, Permissions.ProjectCreation)}
            onChangeDefaultProjectVisibility={this.handleDefaultProjectVisibilityChange}
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
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

export default withCurrentUserContext(ProjectManagementApp);
