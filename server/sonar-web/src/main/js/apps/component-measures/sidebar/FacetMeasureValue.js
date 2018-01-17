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
/*:: import type { MeasureEnhanced } from '../../../components/measure/types'; */

export default function FacetMeasureValue({ measure } /*: { measure: MeasureEnhanced } */) {
  if (isDiffMetric(measure.metric.key)) {
    return (
      <div
        id={`measure-${measure.metric.key}-leak`}
        className="domain-measures-value domain-measures-leak">
        <Measure
          value={measure.leak}
          metricKey={measure.metric.key}
          metricType={measure.metric.type}
        />
      </div>
    );
  }

  return (
    <div id={`measure-${measure.metric.key}-value`} className="domain-measures-value">
      <Measure
        value={measure.value}
        metricKey={measure.metric.key}
        metricType={measure.metric.type}
      />
    </div>
  );
}
