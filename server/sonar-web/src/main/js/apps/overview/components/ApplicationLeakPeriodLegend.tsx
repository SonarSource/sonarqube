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
import Tooltip from '../../../components/controls/Tooltip';
import DateTooltipFormatter from '../../../components/intl/DateTooltipFormatter';
import { getApplicationLeak } from '../../../api/application';
import { translate } from '../../../helpers/l10n';

interface Props {
  component: string;
}

interface State {
  leaks?: Array<{ date: string; project: string; projectName: string }>;
}

export default class ApplicationLeakPeriodLegend extends React.Component<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.component !== this.props.component) {
      this.setState({ leaks: undefined });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchLeaks = (visible: boolean) => {
    if (visible && this.state.leaks) {
      getApplicationLeak(this.props.component).then(
        leaks => {
          if (this.mounted) {
            this.setState({ leaks });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ leaks: [] });
          }
        }
      );
    }
  };

  renderOverlay = () =>
    this.state.leaks != null ? (
      <ul className="text-left">
        {this.state.leaks.map(leak => (
          <li key={leak.project}>
            {leak.projectName}: <DateTooltipFormatter date={leak.date} />
          </li>
        ))}
      </ul>
    ) : (
      <i className="spinner spinner-margin" />
    );

  render() {
    return (
      <Tooltip onVisibleChange={this.fetchLeaks} overlay={this.renderOverlay()}>
        <div className="overview-legend  overview-legend-spaced-line">
          {translate('issues.leak_period')}
        </div>
      </Tooltip>
    );
  }
}
