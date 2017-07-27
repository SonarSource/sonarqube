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
// @flow
import React from 'react';
import ComponentCell from './ComponentCell';
import MeasureCell from './MeasureCell';
import type { Component } from '../../types';
import type { Metric } from '../../../../store/metrics/actions';

type Props = {
  component: Component,
  isSelected: boolean,
  onClick: Component => void,
  otherMetrics: Array<Metric>,
  metric: Metric
};

export default class ComponentsListRow extends React.PureComponent {
  props: Props;

  handleClick = () => this.props.onClick(this.props.component);

  render() {
    const { component } = this.props;
    const otherMeasures = this.props.otherMetrics.map(metric => {
      const measure = component.measures.find(measure => measure.metric === metric.key);
      return { ...measure, metric };
    });
    return (
      <tr>
        <ComponentCell
          component={component}
          isSelected={this.props.isSelected}
          onClick={this.handleClick}
        />

        <MeasureCell component={component} metric={this.props.metric} />

        {otherMeasures.map(measure =>
          <MeasureCell
            key={measure.metric.key}
            component={{
              ...component,
              value: measure.value,
              leak: measure.leak
            }}
            metric={measure.metric}
          />
        )}
      </tr>
    );
  }
}
