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

import { NumericalCell } from '~design-system';
import Measure from '~sonar-aligned/components/measure/Measure';
import { getCCTMeasureValue, isDiffMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { ComponentMeasureEnhanced, MeasureEnhanced, Metric } from '../../../types/types';

interface Props {
  branchLike?: BranchLike;
  component: ComponentMeasureEnhanced;
  measure?: MeasureEnhanced;
  metric: Metric;
}

export default function MeasureCell({ component, measure, metric, branchLike }: Readonly<Props>) {
  const getValue = (item: { leak?: string; value?: string }) =>
    isDiffMetric(metric.key) ? item.leak : item.value;

  const rawValue = getValue(measure || component);
  const value = getCCTMeasureValue(metric.key, rawValue);

  return (
    <NumericalCell className="sw-py-3">
      <Measure
        branchLike={branchLike}
        componentKey={component.key}
        metricKey={metric.key}
        metricType={metric.type}
        value={value}
        small
      />
    </NumericalCell>
  );
}
