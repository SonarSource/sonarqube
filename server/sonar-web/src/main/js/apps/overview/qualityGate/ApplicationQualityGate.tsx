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
import { keyBy } from 'lodash';
import ApplicationQualityGateProject from './ApplicationQualityGateProject';
import Level from '../../../components/ui/Level';
import { getApplicationQualityGate, ApplicationProject } from '../../../api/quality-gates';
import { translate } from '../../../helpers/l10n';
import { LightComponent, Metric } from '../../../app/types';

interface Props {
  component: LightComponent;
}

type State = {
  loading: boolean;
  metrics?: { [key: string]: Metric };
  projects?: ApplicationProject[];
  status?: string;
};

export default class ApplicationQualityGate extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

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

  fetchDetails = () => {
    const { component } = this.props;
    this.setState({ loading: true });
    getApplicationQualityGate({
      application: component.key,
      organization: component.organization
    }).then(
      ({ status, projects, metrics }) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            metrics: keyBy(metrics, 'key'),
            status,
            projects
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const { metrics, status, projects } = this.state;

    return (
      <div className="overview-quality-gate" id="overview-quality-gate">
        <h2 className="overview-title">
          {translate('overview.quality_gate')}
          {this.state.loading && <i className="spinner spacer-left" />}
          {status != null && <Level level={status} />}
        </h2>

        {projects &&
          metrics && (
            <div
              id="overview-quality-gate-conditions-list"
              className="overview-quality-gate-conditions-list clearfix">
              {projects
                .filter(project => project.status !== 'OK')
                .map(project => (
                  <ApplicationQualityGateProject
                    key={project.key}
                    metrics={metrics}
                    project={project}
                  />
                ))}
            </div>
          )}
      </div>
    );
  }
}
