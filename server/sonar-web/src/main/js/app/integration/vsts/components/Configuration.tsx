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
import { searchProjects } from '../../../../api/components';

interface Settings {
  project: string;
}

interface Props {
  widgetHelpers: any;
}

interface State {
  loading: boolean;
  organizations?: Array<{ key: string; name: string }>;
  projects?: Array<{ label: string; value: string }>;
  settings: Settings;
  widgetConfigurationContext?: any;
}

declare const VSS: any;

export default class Configuration extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, settings: { project: '' } };

  componentDidMount() {
    this.mounted = true;
    this.props.widgetHelpers.IncludeWidgetConfigurationStyles();
    VSS.register('e56c6ff0-c6f9-43d0-bdef-b3f1aa0dc6dd', () => {
      return { load: this.load, onSave: this.onSave };
    });
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  load = (widgetSettings: any, widgetConfigurationContext: any) => {
    const settings: Settings = JSON.parse(widgetSettings.customSettings.data);
    if (this.mounted) {
      this.setState({ settings: settings || {}, widgetConfigurationContext });
      this.fetchProjects();
    }
    return this.props.widgetHelpers.WidgetStatusHelper.Success();
  };

  onSave = () => {
    if (!this.state.settings || !this.state.settings.project) {
      return this.props.widgetHelpers.WidgetConfigurationSave.Invalid();
    }
    return this.props.widgetHelpers.WidgetConfigurationSave.Valid({
      data: JSON.stringify(this.state.settings)
    });
  };

  fetchProjects = (organization?: string) => {
    this.setState({ loading: true });
    searchProjects({ organization, ps: 100 }).then(
      ({ components }) => {
        if (this.mounted) {
          this.setState({
            projects: components.map(c => ({ label: c.name, value: c.key })),
            loading: false
          });
        }
      },
      () => {
        this.setState({
          projects: [],
          loading: false
        });
      }
    );
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
      const eventArgs = widgetHelpers.WidgetEvent.Args({ data: JSON.stringify(settings) });
      widgetConfigurationContext.notify(eventName, eventArgs);
    }
  };

  render() {
    const { projects, loading, settings } = this.state;
    if (loading) {
      return (
        <div className="vsts-loading">
          <i className="spinner global-loading-spinner" />
        </div>
      );
    }
    return (
      <div className="widget-configuration">
        <div className="dropdown" id="project">
          <label>SonarCloud project</label>
          <div className="wrapper">
            <select
              onBlur={this.handleProjectChange}
              onChange={this.handleProjectChange}
              value={settings.project}>
              <option disabled={true} hidden={true} value="">
                Select a project...
              </option>
              {projects &&
                projects.map(project => (
                  <option key={project.value} value={project.value}>
                    {project.label}
                  </option>
                ))}
            </select>
          </div>
        </div>
      </div>
    );
  }
}
