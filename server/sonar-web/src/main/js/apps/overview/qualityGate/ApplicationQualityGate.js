/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { keyBy } from 'lodash';
import ApplicationQualityGateProject from './ApplicationQualityGateProject';
import Level from '../../../components/ui/Level';
import { getApplicationQualityGate } from '../../../api/quality-gates';
import { translate } from '../../../helpers/l10n';

type Props = {
  component: { key: string }
};

type State = {
  loading: boolean,
  metrics?: { [string]: Object },
  projects?: Array<{
    conditions: Array<Object>,
    key: string,
    name: string,
    status: string
  }>,
  status?: string
};

export default class ApplicationQualityGate extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State = {
    loading: true
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

  fetchDetails = () => {
    this.setState({ loading: true });
    getApplicationQualityGate(this.props.component.key).then(({ status, projects, metrics }) => {
      if (this.mounted) {
        this.setState({
          loading: false,
          metrics: keyBy(metrics, 'key'),
          status,
          projects
        });
      }
    });
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

        {projects != null &&
          <div
            id="overview-quality-gate-conditions-list"
            className="overview-quality-gate-conditions-list clearfix">
            {projects
              .filter(project => project.status !== 'OK')
              .map(project =>
                <ApplicationQualityGateProject
                  key={project.key}
                  metrics={metrics}
                  project={project}
                />
              )}
          </div>}
      </div>
    );
  }
}
