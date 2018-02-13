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
import { BadgeColors, BadgeType, BadgeOptions } from './utils';
import Select from '../../../components/controls/Select';
import { fetchWebApi } from '../../../api/web-api';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { Metric } from '../../../app/types';

interface Props {
  className?: string;
  metrics: { [key: string]: Metric };
  options: BadgeOptions;
  type: BadgeType;
  updateOptions: (options: Partial<BadgeOptions>) => void;
}

interface State {
  badgeMetrics: string[];
}

export default class BadgeParams extends React.PureComponent<Props> {
  mounted = false;

  state: State = { badgeMetrics: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchBadgeMetrics();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchBadgeMetrics() {
    fetchWebApi(false).then(
      webservices => {
        if (this.mounted) {
          const domain = webservices.find(domain => domain.path === 'api/project_badges');
          const ws = domain && domain.actions.find(ws => ws.key === 'measure');
          const param = ws && ws.params && ws.params.find(param => param.key === 'metric');
          if (param && param.possibleValues) {
            this.setState({ badgeMetrics: param.possibleValues });
          }
        }
      },
      () => {}
    );
  }

  getColorOptions = () =>
    ['white', 'black', 'orange'].map(color => ({
      label: translate('overview.badges.options.colors', color),
      value: color
    }));

  getMetricOptions = () =>
    this.state.badgeMetrics.map(key => {
      const metric = this.props.metrics[key];
      return {
        value: key,
        label: metric ? getLocalizedMetricName(metric) : key
      };
    });

  handleColorChange = ({ value }: { value: BadgeColors }) =>
    this.props.updateOptions({ color: value });

  handleMetricChange = ({ value }: { value: string }) =>
    this.props.updateOptions({ metric: value });

  render() {
    const { className, options, type } = this.props;
    switch (type) {
      case BadgeType.marketing:
        return (
          <div className={className}>
            <label className="big-spacer-right" htmlFor="badge-color">
              {translate('color')}
            </label>
            <Select
              className="input-medium"
              clearable={false}
              name="badge-color"
              onChange={this.handleColorChange}
              options={this.getColorOptions()}
              searchable={false}
              value={options.color}
            />
          </div>
        );
      case BadgeType.measure:
        return (
          <div className={className}>
            <label className="big-spacer-right" htmlFor="badge-metric">
              {translate('overview.badges.metric')}
            </label>
            <Select
              className="input-medium"
              clearable={false}
              name="badge-metric"
              onChange={this.handleMetricChange}
              options={this.getMetricOptions()}
              searchable={false}
              value={options.metric}
            />
          </div>
        );
      default:
        return null;
    }
  }
}
