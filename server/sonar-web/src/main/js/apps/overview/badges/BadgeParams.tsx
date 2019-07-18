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
import * as classNames from 'classnames';
import * as React from 'react';
import Select from 'sonar-ui-common/components/controls/Select';
import { getLocalizedMetricName, translate } from 'sonar-ui-common/helpers/l10n';
import { fetchWebApi } from '../../../api/web-api';
import { BadgeColors, BadgeFormats, BadgeOptions, BadgeType } from './utils';

interface Props {
  className?: string;
  metrics: T.Dict<T.Metric>;
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

  getColorOptions = () => {
    return ['white', 'black', 'orange'].map(color => ({
      label: translate('overview.badges.options.colors', color),
      value: color
    }));
  };

  getFormatOptions = () => {
    return ['md', 'url'].map(format => ({
      label: translate('overview.badges.options.formats', format),
      value: format
    }));
  };

  getMetricOptions = () => {
    return this.state.badgeMetrics.map(key => {
      const metric = this.props.metrics[key];
      return {
        value: key,
        label: metric ? getLocalizedMetricName(metric) : key
      };
    });
  };

  handleColorChange = ({ value }: { value: BadgeColors }) => {
    this.props.updateOptions({ color: value });
  };

  handleFormatChange = ({ value }: { value: BadgeFormats }) => {
    this.props.updateOptions({ format: value });
  };

  handleMetricChange = ({ value }: { value: string }) => {
    this.props.updateOptions({ metric: value });
  };

  renderBadgeType = (type: BadgeType, options: BadgeOptions) => {
    if (type === BadgeType.marketing) {
      return (
        <>
          <label className="spacer-right" htmlFor="badge-color">
            {translate('color')}:
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
        </>
      );
    } else if (type === BadgeType.measure) {
      return (
        <>
          <label className="spacer-right" htmlFor="badge-metric">
            {translate('overview.badges.metric')}:
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
        </>
      );
    } else {
      return null;
    }
  };

  render() {
    const { className, options, type } = this.props;
    return (
      <div className={className}>
        {this.renderBadgeType(type, options)}

        <label
          className={classNames('spacer-right', {
            'big-spacer-left': type !== BadgeType.qualityGate
          })}
          htmlFor="badge-format">
          {translate('format')}:
        </label>
        <Select
          className="input-medium"
          clearable={false}
          name="badge-format"
          onChange={this.handleFormatChange}
          options={this.getFormatOptions()}
          searchable={false}
          value={this.props.options.format || 'md'}
        />
      </div>
    );
  }
}
