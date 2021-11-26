/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { Location } from 'history';
import * as React from 'react';
import { InjectedRouter } from 'react-router';
import { editApplication, getApplicationDetails, refreshApplication } from '../../api/application';
import addGlobalSuccessMessage from '../../app/utils/addGlobalSuccessMessage';
import { translate } from '../../helpers/l10n';
import { Application, ApplicationProject } from '../../types/application';
import ApplicationConsoleAppRenderer from './ApplicationConsoleAppRenderer';
import { ApplicationBranch } from './utils';

interface Props {
  component: Pick<T.Component, 'key' | 'canBrowseAllChildProjects'>;
  location: Location;
  router: Pick<InjectedRouter, 'replace'>;
}

interface State {
  application?: Application;
  loading: boolean;
}

export default class ApplicationConsoleApp extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: false
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchDetails();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component.key !== this.props.component.key) {
      this.fetchDetails();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  updateApplicationState = (buildNewFields: (prevApp: Application) => Partial<Application>) => {
    this.setState(state => {
      if (state.application) {
        return { application: { ...state.application, ...buildNewFields(state.application) } };
      }

      return null;
    });
  };

  fetchDetails = async () => {
    try {
      const application = await getApplicationDetails(this.props.component.key);
      if (this.mounted) {
        this.setState({ application, loading: false });
      }
    } catch {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    }
  };

  handleRefreshClick = async () => {
    if (this.state.application) {
      await refreshApplication(this.state.application.key);
      addGlobalSuccessMessage(translate('application_console.refresh_started'));
    }
  };

  handleEdit = async (name: string, description: string) => {
    if (this.state.application) {
      await editApplication(this.state.application.key, name, description);
    }
    if (this.mounted) {
      this.updateApplicationState(() => ({ name, description }));
    }
  };

  handleAddProject = (project: ApplicationProject) => {
    this.updateApplicationState(prevApp => ({ projects: [...prevApp.projects, project] }));
  };

  handleRemoveProject = (projectKey: string) => {
    this.updateApplicationState(prevApp => ({
      projects: prevApp.projects.filter(p => p.key !== projectKey)
    }));
  };

  handleUpdateBranches = (branches: ApplicationBranch[]) => {
    this.updateApplicationState(() => ({ branches }));
  };

  render() {
    const { component } = this.props;
    const { application, loading } = this.state;
    if (!application) {
      // when application is not found
      return null;
    }

    return (
      <ApplicationConsoleAppRenderer
        loading={loading}
        application={application}
        canBrowseAllChildProjects={Boolean(component.canBrowseAllChildProjects)}
        onAddProject={this.handleAddProject}
        onEdit={this.handleEdit}
        onRefresh={this.handleRefreshClick}
        onRemoveProject={this.handleRemoveProject}
        onUpdateBranches={this.handleUpdateBranches}
      />
    );
  }
}
