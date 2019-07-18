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
import { Component, searchProjects } from '@sqapi/components';
import { getCurrentUser } from '@sqapi/users';
import * as React from 'react';
import {
  parseWidgetSettings,
  serializeWidgetSettings,
  Settings,
  VSTSConfigurationContext,
  VSTSWidgetSettings
} from '../utils';
import LoginForm from './LoginForm';
import ProjectSelector from './ProjectSelector';

interface Props {
  contribution: string;
  widgetHelpers: any;
}

interface State {
  currentUser?: T.CurrentUser;
  loading: boolean;
  projects: Component[];
  settings: Settings;
  selectedProject?: Component;
  widgetConfigurationContext?: VSTSConfigurationContext;
}

declare const VSS: {
  register: (contributionId: string, callback: Function) => void;
  resize: Function;
};

const PAGE_SIZE = 10;

export default class Configuration extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, projects: [], settings: { project: '' } };

  componentDidMount() {
    this.mounted = true;
    VSS.register(this.props.contribution, () => {
      return { load: this.load, onSave: this.onSave };
    });
  }

  componentDidUpdate() {
    VSS.resize();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  load = (
    widgetSettings: VSTSWidgetSettings,
    widgetConfigurationContext: VSTSConfigurationContext
  ) => {
    const settings = parseWidgetSettings(widgetSettings);
    if (this.mounted) {
      this.setState({ settings: settings || {}, widgetConfigurationContext });
      this.fetchInitialData();
    }
    return this.props.widgetHelpers.WidgetStatusHelper.Success();
  };

  onSave = () => {
    const { settings } = this.state;
    if (!settings.project) {
      return this.props.widgetHelpers.WidgetConfigurationSave.Invalid();
    }
    return this.props.widgetHelpers.WidgetConfigurationSave.Valid(
      serializeWidgetSettings(settings)
    );
  };

  fetchInitialData = () => {
    this.setState({ loading: true });
    getCurrentUser()
      .then(currentUser => {
        this.setState({ currentUser });
        const params: { ps: number; filter?: string } = { ps: PAGE_SIZE };
        if (currentUser.isLoggedIn) {
          params.filter = 'isFavorite';
        }
        return searchProjects(params);
      })
      .then(this.handleSearchProjectsResult, this.stopLoading);
  };

  handleReload = () => {
    this.fetchInitialData();
  };

  handleProjectChange = (
    event: React.ChangeEvent<HTMLSelectElement> | React.FocusEvent<HTMLSelectElement>
  ) => {
    const { value } = event.currentTarget;
    this.setState(
      ({ settings }) => ({ settings: { ...settings, project: value } }),
      this.notifyChange
    );
  };

  notifyChange = ({ settings, widgetConfigurationContext } = this.state) => {
    const { widgetHelpers } = this.props;
    if (widgetConfigurationContext && widgetConfigurationContext.notify) {
      const eventName = widgetHelpers.WidgetEvent.ConfigurationChange;
      const eventArgs = widgetHelpers.WidgetEvent.Args(serializeWidgetSettings(settings));
      widgetConfigurationContext.notify(eventName, eventArgs);
    }
  };

  handleProjectSearch = (query: string) => {
    const searchParams: { ps: number; filter?: string } = { ps: PAGE_SIZE };
    if (query) {
      searchParams.filter = query;
    }
    return searchProjects(searchParams).then(this.handleSearchProjectsResult, this.stopLoading);
  };

  handleProjectSelect = (project: Component) => {
    this.setState(
      ({ settings }) => ({
        selectedProject: project,
        settings: { ...settings, project: project.key }
      }),
      this.notifyChange
    );
  };

  handleSearchProjectsResult = ({ components }: { components: Component[] }) => {
    if (this.mounted) {
      this.setState({ loading: false, projects: components });
    }
  };

  stopLoading = () => {
    this.setState({ loading: false });
  };

  render() {
    const { currentUser, projects, loading, selectedProject, settings } = this.state;
    if (loading) {
      return (
        <div className="vsts-loading">
          <i className="spinner global-loading-spinner" />
        </div>
      );
    }

    const isLoggedIn = Boolean(currentUser && currentUser.isLoggedIn);
    const selected = selectedProject || projects.find(project => project.key === settings.project);
    return (
      <div className="widget-configuration vsts-configuration bowtie">
        <div className="dropdown config-settings-field" id="sonarcloud-project">
          <label>SonarCloud project</label>
          <ProjectSelector
            isLoggedIn={isLoggedIn}
            onQueryChange={this.handleProjectSearch}
            onSelect={this.handleProjectSelect}
            projects={projects}
            selected={selected}
          />
        </div>
        {!isLoggedIn && (
          <div className="config-settings-field">
            <label>You must be logged in to see your private projects :</label>
            <LoginForm onReload={this.handleReload} />
          </div>
        )}
      </div>
    );
  }
}
