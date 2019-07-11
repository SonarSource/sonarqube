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
import IssueTypeIcon from 'sonar-ui-common/components/icons/IssueTypeIcon';
import { getLocalizedMetricName } from 'sonar-ui-common/helpers/l10n';
import Measure from '../../measure/Measure';

export interface MeasureWithMetric {
  metric: T.Metric;
  value?: string;
}

interface Props {
  measure: MeasureWithMetric;
}

export default function MeasuresOverlayMeasure({ measure }: Props) {
  return (
    <div
      className="measure measure-one-line"
      data-metric={measure.metric.key}
      key={measure.metric.key}>
      <span className="measure-name">
        {['bugs', 'vulnerabilities', 'code_smells'].includes(measure.metric.key) && (
          <IssueTypeIcon className="little-spacer-right" query={measure.metric.key} />
        )}
        {getLocalizedMetricName(measure.metric)}
      </span>
      <span className="measure-value">
        <Measure
          metricKey={measure.metric.key}
          metricType={measure.metric.type}
          small={true}
          value={measure.value}
        />
      </span>
    </div>
  );
}
