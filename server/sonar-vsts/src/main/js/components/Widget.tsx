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
import QGWidget from './QGWidget';
import LoginForm from './LoginForm';
import { getMeasuresAndMeta } from '../../../../../sonar-web/src/main/js/api/measures';
import { Settings } from '../utils';

interface Props {
  settings: Settings;
}

interface State {
  component?: T.ComponentMeasure;
  loading: boolean;
  metrics?: T.Metric[];
  unauthorized: boolean;
}
export default class Widget extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, unauthorized: false };

  componentDidMount() {
    this.mounted = true;
    const { settings } = this.props;
    if (settings.project) {
      this.fetchProjectMeasures(settings.project);
    } else {
      this.setState({ loading: false });
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    const { project } = nextProps.settings;
    if (project !== this.props.settings.project) {
      if (project) {
        this.fetchProjectMeasures(project);
      } else {
        this.setState({ component: undefined });
      }
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchProjectMeasures = (project: string) => {
    this.setState({ loading: true });
    getMeasuresAndMeta(project, ['alert_status'], { additionalFields: 'metrics' }).then(
      ({ component, metrics }) => {
        if (this.mounted) {
          this.setState({ component, loading: false, metrics, unauthorized: false });
        }
      },
      response => {
        if (response && response.response.status === 403) {
          this.setState({ loading: false, unauthorized: true });
        } else {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleReload = () => {
    const { settings } = this.props;
    if (settings.project) {
      this.fetchProjectMeasures(settings.project);
    }
  };

  render() {
    const { component, loading, metrics, unauthorized } = this.state;
    if (loading) {
      return (
        <div className="vsts-loading">
          <i className="spinner global-loading-spinner" />
        </div>
      );
    }

    if (unauthorized) {
      return (
        <div className="widget">
          <LoginForm onReload={this.handleReload} title="Authentication on SonarCloud required" />
        </div>
      );
    }

    if (!component || !metrics) {
      return (
        <div className="vsts-widget-configure widget">
          <h2 className="title">Code Quality</h2>
          <div className="content">
            <div>Configure widget</div>
            <img
              alt=""
              src="https://cdn.vsassets.io/v/20180301T143409/_content/Dashboards/unconfigured-small.png"
            />
          </div>
        </div>
      );
    }

    return <QGWidget component={component} />;
  }
}
