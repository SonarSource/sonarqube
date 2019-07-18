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
import { keyBy } from 'lodash';
import * as React from 'react';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Level from 'sonar-ui-common/components/ui/Level';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ApplicationProject, getApplicationQualityGate } from '../../../api/quality-gates';
import DocTooltip from '../../../components/docs/DocTooltip';
import ApplicationQualityGateProject from './ApplicationQualityGateProject';

interface Props {
  branch?: T.LongLivingBranch;
  component: T.LightComponent;
}

type State = {
  loading: boolean;
  metrics?: T.Dict<T.Metric>;
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
    const { branch, component } = this.props;
    this.setState({ loading: true });
    getApplicationQualityGate({
      application: component.key,
      branch: branch ? branch.name : undefined,
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
          <DocTooltip
            className="spacer-left"
            doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/quality-gates/project-homepage-quality-gate.md')}
          />
          {status != null && <Level className="big-spacer-left" level={status} />}
          {status === 'WARN' && (
            <HelpTooltip
              className="little-spacer-left"
              overlay={translate('quality_gates.conditions.warning.tooltip')}
            />
          )}
        </h2>

        {projects && metrics && (
          <div
            className="overview-quality-gate-conditions-list clearfix"
            id="overview-quality-gate-conditions-list">
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
