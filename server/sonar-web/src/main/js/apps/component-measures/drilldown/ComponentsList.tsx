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
import { getLocalizedMetricName } from 'sonar-ui-common/helpers/l10n';
import { complementary } from '../config/complementary';
import { View } from '../utils';
import ComponentsListRow from './ComponentsListRow';
import EmptyResult from './EmptyResult';

interface Props {
  branchLike?: T.BranchLike;
  components: T.ComponentMeasureEnhanced[];
  onClick: (component: string) => void;
  metric: T.Metric;
  metrics: T.Dict<T.Metric>;
  rootComponent: T.ComponentMeasure;
  selectedComponent?: string;
  view: View;
}

export default function ComponentsList({ components, metric, metrics, ...props }: Props) {
  if (!components.length) {
    return <EmptyResult />;
  }

  const otherMetrics = (complementary[metric.key] || []).map(key => metrics[key]);
  return (
    <table className="data zebra zebra-hover">
      {otherMetrics.length > 0 && (
        <thead>
          <tr>
            <th>&nbsp;</th>
            <th className="text-right">
              <span className="small">{getLocalizedMetricName(metric)}</span>
            </th>
            {otherMetrics.map(metric => (
              <th className="text-right" key={metric.key}>
                <span className="small">{getLocalizedMetricName(metric)}</span>
              </th>
            ))}
          </tr>
        </thead>
      )}

      <tbody>
        {components.map(component => (
          <ComponentsListRow
            component={component}
            isSelected={component.key === props.selectedComponent}
            key={component.key}
            metric={metric}
            otherMetrics={otherMetrics}
            {...props}
          />
        ))}
      </tbody>
    </table>
  );
}
