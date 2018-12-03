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
import * as classNames from 'classnames';
import ComponentCell from './ComponentCell';
import MeasureCell from './MeasureCell';
import { View } from '../utils';

interface Props {
  branchLike?: T.BranchLike;
  component: T.ComponentMeasureEnhanced;
  isSelected: boolean;
  onClick: (component: string) => void;
  otherMetrics: T.Metric[];
  metric: T.Metric;
  rootComponent: T.ComponentMeasure;
  view: View;
}

export default function ComponentsListRow(props: Props) {
  const { branchLike, component, rootComponent } = props;
  const otherMeasures = props.otherMetrics.map(metric => {
    const measure = component.measures.find(measure => measure.metric.key === metric.key);
    return { ...measure, metric };
  });
  const rowClass = classNames('measure-details-component-row', {
    selected: props.isSelected
  });
  return (
    <tr className={rowClass}>
      <ComponentCell
        branchLike={branchLike}
        component={component}
        metric={props.metric}
        onClick={props.onClick}
        rootComponent={rootComponent}
        view={props.view}
      />

      <MeasureCell component={component} metric={props.metric} />

      {otherMeasures.map(measure => (
        <MeasureCell
          component={component}
          key={measure.metric.key}
          measure={measure}
          metric={measure.metric}
        />
      ))}
    </tr>
  );
}
