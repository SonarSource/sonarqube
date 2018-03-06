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
import QGWidget from './QGWidget';
import { getMeasuresAndMeta, MeasureComponent } from '../../../../api/measures';
import { Metric } from '../../../types';

interface Props {
  widgetHelpers: any;
}

interface State {
  component?: MeasureComponent;
  loading: boolean;
  metrics?: Metric[];
}

declare const VSS: any;

export default class Widget extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.props.widgetHelpers.IncludeWidgetStyles();
    VSS.register('3c598f25-01c1-4c09-97c6-926476882688', () => {
      return { load: this.load, reload: this.load };
    });
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  load = (widgetSettings: any) => {
    const settings = JSON.parse(widgetSettings.customSettings.data);
    if (this.mounted) {
      if (settings && settings.project) {
        this.fetchProjectMeasures(settings.project);
      } else {
        this.setState({ loading: false });
      }
    }
    return this.props.widgetHelpers.WidgetStatusHelper.Success();
  };

  fetchProjectMeasures = (project: string) => {
    this.setState({ loading: true });
    getMeasuresAndMeta(project, ['alert_status'], { additionalFields: 'metrics' }).then(
      ({ component, metrics }) => {
        if (this.mounted) {
          this.setState({ component, loading: false, metrics });
        }
      },
      () => {
        this.setState({ loading: false });
      }
    );
  };

  render() {
    const { component, loading, metrics } = this.state;
    if (loading) {
      return (
        <div className="vsts-loading">
          <i className="spinner global-loading-spinner" />
        </div>
      );
    }

    if (!component || !metrics) {
      return (
        <div className="vsts-widget-configure widget">
          <h2 className="title">Quality Widget</h2>
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

    return <QGWidget component={component} metrics={metrics} />;
  }
}
