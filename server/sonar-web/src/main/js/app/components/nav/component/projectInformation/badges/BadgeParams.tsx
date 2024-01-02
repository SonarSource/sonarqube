/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { fetchWebApi } from '../../../../../../api/web-api';
import Select from '../../../../../../components/controls/Select';
import { getLocalizedMetricName, translate } from '../../../../../../helpers/l10n';
import { Dict, Metric } from '../../../../../../types/types';
import withMetricsContext from '../../../../metrics/withMetricsContext';
import { BadgeFormats, BadgeOptions, BadgeType } from './utils';

interface Props {
  className?: string;
  metrics: Dict<Metric>;
  options: BadgeOptions;
  type: BadgeType;
  updateOptions: (options: Partial<BadgeOptions>) => void;
}

interface State {
  badgeMetrics: string[];
}

export class BadgeParams extends React.PureComponent<Props> {
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
      (webservices) => {
        if (this.mounted) {
          const domain = webservices.find((d) => d.path === 'api/project_badges');
          const ws = domain && domain.actions.find((w) => w.key === 'measure');
          const param = ws && ws.params && ws.params.find((p) => p.key === 'metric');
          if (param && param.possibleValues) {
            this.setState({ badgeMetrics: param.possibleValues });
          }
        }
      },
      () => {}
    );
  }

  getColorOptions = () => {
    return ['white', 'black', 'orange'].map((color) => ({
      label: translate('overview.badges.options.colors', color),
      value: color,
    }));
  };

  getFormatOptions = () => {
    return ['md', 'url'].map((format) => ({
      label: translate('overview.badges.options.formats', format),
      value: format as BadgeFormats,
    }));
  };

  getMetricOptions = () => {
    return this.state.badgeMetrics.map((key) => {
      const metric = this.props.metrics[key];
      return {
        value: key,
        label: metric ? getLocalizedMetricName(metric) : key,
      };
    });
  };

  handleFormatChange = ({ value }: { value: BadgeFormats }) => {
    this.props.updateOptions({ format: value });
  };

  handleMetricChange = ({ value }: { value: string }) => {
    this.props.updateOptions({ metric: value });
  };

  renderBadgeType = (type: BadgeType, options: BadgeOptions) => {
    if (type === BadgeType.measure) {
      const metricOptions = this.getMetricOptions();
      return (
        <>
          <label className="spacer-right" htmlFor="badge-metric">
            {translate('overview.badges.metric')}:
          </label>
          <Select
            className="input-medium it__metric-badge-select"
            name="badge-metric"
            isSearchable={false}
            onChange={this.handleMetricChange}
            options={metricOptions}
            value={metricOptions.find((o) => o.value === options.metric)}
          />
        </>
      );
    }
    return null;
  };

  render() {
    const { className, options, type } = this.props;
    const formatOptions = this.getFormatOptions();
    return (
      <div className={className}>
        {this.renderBadgeType(type, options)}

        <label
          className={classNames('spacer-right', {
            'spacer-top': type !== BadgeType.qualityGate,
          })}
          htmlFor="badge-format"
        >
          {translate('format')}:
        </label>
        <Select
          className="input-medium"
          name="badge-format"
          isSearchable={false}
          onChange={this.handleFormatChange}
          options={formatOptions}
          value={formatOptions.find((o) => o.value === options.format)}
          defaultValue={formatOptions.find((o) => o.value === 'md')}
        />
      </div>
    );
  }
}

export default withMetricsContext(BadgeParams);
