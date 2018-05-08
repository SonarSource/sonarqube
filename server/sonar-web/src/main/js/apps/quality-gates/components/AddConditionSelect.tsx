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
import { sortBy } from 'lodash';
import Select from '../../../components/controls/Select';
import { translate, getLocalizedMetricName, getLocalizedMetricDomain } from '../../../helpers/l10n';
import { Metric } from '../../../app/types';

interface Props {
  metrics: Metric[];
  onAddCondition: (metric: string) => void;
}

interface Option {
  disabled?: boolean;
  domain?: string;
  label: string;
  value: string;
}

export default class AddConditionSelect extends React.PureComponent<Props> {
  handleChange = (option: Option) => {
    this.props.onAddCondition(option.value);
  };

  render() {
    const { metrics } = this.props;

    const options: Option[] = sortBy(
      metrics.map(metric => ({
        value: metric.key,
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
          value: option.domain,
          label: getLocalizedMetricDomain(option.domain),
          disabled: true
        });
      }
      optionsWithDomains.push(option);
    });

    return (
      <div className="big-spacer-top panel bg-muted">
        <Select
          className="text-middle input-large"
          onChange={this.handleChange}
          options={optionsWithDomains}
          placeholder={translate('quality_gates.add_condition')}
        />
      </div>
    );
  }
}
