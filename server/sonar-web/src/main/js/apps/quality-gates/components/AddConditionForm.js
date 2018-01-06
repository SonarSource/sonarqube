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
import React from 'react';
import { omitBy, map, sortBy } from 'lodash';
import Select from '../../../components/controls/Select';
import { translate, getLocalizedMetricName, getLocalizedMetricDomain } from '../../../helpers/l10n';

export default function AddConditionForm({ metrics, onSelect }) {
  function handleChange(option) {
    const metric = option.value;

    // e.target.value = '';
    onSelect(metric);
  }

  const metricsToDisplay = omitBy(metrics, metric => metric.hidden);
  const options = sortBy(
    map(metricsToDisplay, metric => ({
      value: metric.key,
      label: getLocalizedMetricName(metric),
      domain: metric.domain
    })),
    'domain'
  );

  // use "disabled" property to emulate optgroups
  const optionsWithDomains = [];
  options.forEach((option, index, options) => {
    const previous = index > 0 ? options[index - 1] : null;
    if (!previous || previous.domain !== option.domain) {
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
        id="quality-gate-new-condition-metric"
        className="text-middle input-large"
        options={optionsWithDomains}
        placeholder={translate('quality_gates.add_condition')}
        onChange={handleChange}
      />
    </div>
  );
}
