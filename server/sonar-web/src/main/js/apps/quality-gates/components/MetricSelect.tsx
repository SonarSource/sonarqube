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
import { sortBy } from 'lodash';
import Select from '../../../components/controls/Select';
import { translate, getLocalizedMetricName, getLocalizedMetricDomain } from '../../../helpers/l10n';

interface Props {
  metrics: T.Metric[];
  onMetricChange: (metric: T.Metric) => void;
}

interface State {
  value: number;
}

interface Option {
  disabled?: boolean;
  domain?: string;
  label: string;
  value: number;
}

export default class MetricSelect extends React.PureComponent<Props, State> {
  state = { value: -1 };

  handleChange = (option: Option | null) => {
    const value = option ? option.value : -1;
    this.setState({ value });
    this.props.onMetricChange(this.props.metrics[value]);
  };

  render() {
    const { metrics } = this.props;

    const options: Option[] = sortBy(
      metrics.map((metric, index) => ({
        value: index,
        label: getLocalizedMetricName(metric),
        domain: metric.domain
      })),
      'domain'
    );

    // use "disabled" property to emulate optgroups
    const optionsWithDomains: Option[] = [];
    options.forEach((option, index, options) => {
      const previous = index > 0 ? options[index - 1] : null;
      if (option.domain && (!previous || previous.domain !== option.domain)) {
        optionsWithDomains.push({
          value: 0,
          label: getLocalizedMetricDomain(option.domain),
          disabled: true
        });
      }
      optionsWithDomains.push(option);
    });

    return (
      <Select
        className="text-middle"
        id="condition-metric"
        onChange={this.handleChange}
        options={optionsWithDomains}
        placeholder={translate('search.search_for_metrics')}
        value={this.state.value}
      />
    );
  }
}
