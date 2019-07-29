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
import { sortBy } from 'lodash';
import * as React from 'react';
import Select from 'sonar-ui-common/components/controls/Select';
import { getLocalizedMetricDomain, translate } from 'sonar-ui-common/helpers/l10n';
import { getLocalizedMetricNameNoDiffMetric } from '../utils';

interface Props {
  metric?: T.Metric;
  metrics: T.Metric[];
  onMetricChange: (metric: T.Metric) => void;
}

interface Option {
  disabled?: boolean;
  label: string;
  value: string;
}

export default class MetricSelect extends React.PureComponent<Props> {
  handleChange = (option: Option | null) => {
    if (option) {
      const { metrics } = this.props;
      const selectedMetric = metrics.find(metric => metric.key === option.value);
      if (selectedMetric) {
        this.props.onMetricChange(selectedMetric);
      }
    }
  };

  render() {
    const { metric, metrics } = this.props;

    const options: Array<Option & { domain?: string }> = sortBy(
      metrics.map(metric => ({
        value: metric.key,
        label: getLocalizedMetricNameNoDiffMetric(metric),
        domain: metric.domain
      })),
      'domain'
    );

    // Use "disabled" property to emulate optgroups.
    const optionsWithDomains: Option[] = [];
    options.forEach((option, index, options) => {
      const previous = index > 0 ? options[index - 1] : null;
      if (option.domain && (!previous || previous.domain !== option.domain)) {
        optionsWithDomains.push({
          value: '<domain>',
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
        value={metric && metric.key}
      />
    );
  }
}
