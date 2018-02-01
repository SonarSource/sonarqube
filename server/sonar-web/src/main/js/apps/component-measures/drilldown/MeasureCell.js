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
// @flow
import React from 'react';
import Measure from '../../../components/measure/Measure';
import { isDiffMetric } from '../../../helpers/measures';
/*:: import type { ComponentEnhanced } from '../types'; */
/*:: import type { MeasureEnhanced } from '../../../components/measure/types'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */

/*:: type Props = {
  component: ComponentEnhanced,
  measure?: MeasureEnhanced,
  metric: Metric
}; */

export default function MeasureCell({ component, measure, metric } /*: Props */) {
  const getValue = (item /*: { leak?: ?string; value?: string } */) =>
    isDiffMetric(metric.key) ? item.leak : item.value;

  const value = getValue(measure || component);

  return (
    <td className="thin nowrap text-right">
      <span id={`component-measures-component-measure-${component.key}-${metric.key}`}>
        <Measure value={value} metricKey={metric.key} metricType={metric.type} />
      </span>
    </td>
  );
}
