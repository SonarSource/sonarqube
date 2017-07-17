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
import ComponentCell from './ComponentCell';
import MeasureCell from './MeasureCell';

const replaceMeasure = (component, measure) => {
  return {
    ...component,
    value: measure.value,
    leak: measure.leak
  };
};

const ComponentsListRow = ({ component, otherMetrics, isSelected, metric, onClick }) => {
  const handleClick = () => {
    onClick(component);
  };

  const otherMeasures = otherMetrics.map(metric => {
    const measure = component.measures.find(measure => measure.metric === metric.key);
    return { ...measure, metric };
  });

  return (
    <tr>
      <ComponentCell
        component={component}
        isSelected={isSelected}
        onClick={handleClick.bind(this, component)}
      />

      <MeasureCell component={component} metric={metric} />

      {otherMeasures.map(measure =>
        <MeasureCell
          key={measure.metric.key}
          component={replaceMeasure(component, measure)}
          metric={measure.metric}
        />
      )}
    </tr>
  );
};

export default ComponentsListRow;
