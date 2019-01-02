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
import * as classNames from 'classnames';
import { sortBy } from 'lodash';
import Tooltip from '../../../components/controls/Tooltip';
import DateTooltipFormatter from '../../../components/intl/DateTooltipFormatter';
import { getApplicationLeak } from '../../../api/application';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import DateFromNow from '../../../components/intl/DateFromNow';

interface Props {
  branch?: T.LongLivingBranch;
  component: T.LightComponent;
}

interface State {
  leaks?: Array<{ date: string; project: string; projectName: string }>;
}

export default class ApplicationLeakPeriodLegend extends React.Component<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.fetchLeaks();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.component.key !== this.props.component.key) {
      this.setState({ leaks: undefined });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchLeaks = () => {
    if (!this.state.leaks) {
      getApplicationLeak(
        this.props.component.key,
        this.props.branch ? this.props.branch.name : undefined
      ).then(
        leaks => {
          if (this.mounted) {
            this.setState({
              leaks: sortBy(leaks, value => {
                return new Date(value.date);
              })
            });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ leaks: undefined });
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
    const leak = this.state.leaks && this.state.leaks.length > 0 ? this.state.leaks[0] : undefined;
    return (
      <Tooltip overlay={this.renderOverlay()}>
        <div className={classNames('overview-legend', { 'overview-legend-spaced-line': !leak })}>
          {translate('issues.max_new_code_period')}:{' '}
          {leak && (
            <>
              <DateFromNow date={leak.date}>
                {fromNow => <span>{translateWithParameters('overview.started_x', fromNow)}</span>}
              </DateFromNow>
              <br />
              <span className="note">
                {translate('from')}:{leak.projectName}
              </span>
            </>
          )}
        </div>
      </Tooltip>
    );
  }
}
