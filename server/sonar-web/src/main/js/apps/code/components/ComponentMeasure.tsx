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
import Measure from '../../../components/measure/Measure';
import { getLeakValue } from '../../../components/measure/utils';
import { isDiffMetric } from '../../../helpers/measures';

interface Props {
  component: T.ComponentMeasure;
  metric: T.Metric;
}

export default class ComponentMeasure extends React.PureComponent<Props> {
  render() {
    const { component, metric } = this.props;
    const isProject = component.qualifier === 'TRK';
    const isReleasability = metric.key === 'releasability_rating';

    const finalMetricKey = isProject && isReleasability ? 'alert_status' : metric.key;
    const finalMetricType = isProject && isReleasability ? 'LEVEL' : metric.type;

    const measure =
      Array.isArray(component.measures) &&
      component.measures.find(measure => measure.metric === finalMetricKey);

    if (!measure) {
      return measure === false ? <span /> : <span>—</span>;
    }

    const value = isDiffMetric(metric.key) ? getLeakValue(measure) : measure.value;
    return <Measure metricKey={finalMetricKey} metricType={finalMetricType} value={value} />;
  }
}
