/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import ComponentsListRow from './ComponentsListRow';
import EmptyComponentsList from './EmptyComponentsList';
import complementary from '../../config/complementary';
import { getLocalizedMetricName } from '../../../../helpers/l10n';

const ComponentsList = ({ components, metrics, selected, metric, onClick }) => {
  if (!components.length) {
    return <EmptyComponentsList />;
  }

  const otherMetrics = (complementary[metric.key] || []).map(metric => {
    return metrics.find(m => m.key === metric);
  });

  return (
    <table className="data zebra zebra-hover">
      {otherMetrics.length > 0 &&
        <thead>
          <tr>
            <th>&nbsp;</th>
            <th className="text-right">
              <span className="small">
                {getLocalizedMetricName(metric)}
              </span>
            </th>
            {otherMetrics.map(metric =>
              <th key={metric.key} className="text-right">
                <span className="small">
                  {getLocalizedMetricName(metric)}
                </span>
              </th>
            )}
          </tr>
        </thead>}

      <tbody>
        {components.map(component =>
          <ComponentsListRow
            key={component.id}
            component={component}
            otherMetrics={otherMetrics}
            isSelected={component === selected}
            metric={metric}
            onClick={onClick}
          />
        )}
      </tbody>
    </table>
  );
};

export default ComponentsList;
