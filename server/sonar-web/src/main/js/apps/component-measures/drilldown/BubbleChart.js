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
import EmptyResult from './EmptyResult';
import OriginalBubbleChart from '../../../components/charts/BubbleChart';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';
import {
  getLocalizedMetricDomain,
  getLocalizedMetricName,
  translateWithParameters
} from '../../../helpers/l10n';
import { bubbles } from '../config/bubbles';
import type { Component, ComponentEnhanced } from '../types';
import type { Metric } from '../../../store/metrics/actions';

const HEIGHT = 500;

type Props = {|
  component: Component,
  components: Array<ComponentEnhanced>,
  domain: string,
  metrics: { [string]: Metric },
  updateSelected: string => void
|};

export default class BubbleChart extends React.PureComponent {
  props: Props;

  getBubbleMetrics = ({ domain, metrics }: Props) => {
    const conf = bubbles[domain];
    return {
      xMetric: metrics[conf.x],
      yMetric: metrics[conf.y],
      sizeMetric: metrics[conf.size]
    };
  };

  getMeasureVal = (component: ComponentEnhanced, metric: Metric) => {
    const measure = component.measures.find(measure => measure.metric.key === metric.key);
    if (measure) {
      return Number(isDiffMetric(metric.key) ? measure.leak : measure.value);
    }
  };

  getTooltip(
    componentName: string,
    x: number,
    y: number,
    size: number,
    xMetric: Metric,
    yMetric: Metric,
    sizeMetric: Metric
  ) {
    const inner = [
      componentName,
      `${xMetric.name}: ${formatMeasure(x, xMetric.type)}`,
      `${yMetric.name}: ${formatMeasure(y, yMetric.type)}`,
      `${sizeMetric.name}: ${formatMeasure(size, sizeMetric.type)}`
    ].join('<br>');
    return `<div class="text-left">${inner}</div>`;
  }

  handleBubbleClick = (component: ComponentEnhanced) => this.props.updateSelected(component.key);

  renderBubbleChart(xMetric: Metric, yMetric: Metric, sizeMetric: Metric) {
    const items = this.props.components
      .map(component => {
        const x = this.getMeasureVal(component, xMetric);
        const y = this.getMeasureVal(component, yMetric);
        const size = this.getMeasureVal(component, sizeMetric);
        if ((!x && x !== 0) || (!y && y !== 0) || (!size && size !== 0)) {
          return null;
        }
        return {
          x,
          y,
          size,
          link: component,
          tooltip: this.getTooltip(component.name, x, y, size, xMetric, yMetric, sizeMetric)
        };
      })
      .filter(Boolean);

    const formatXTick = tick => formatMeasure(tick, xMetric.type);
    const formatYTick = tick => formatMeasure(tick, yMetric.type);

    return (
      <OriginalBubbleChart
        items={items}
        height={HEIGHT}
        padding={[25, 60, 50, 60]}
        formatXTick={formatXTick}
        formatYTick={formatYTick}
        onBubbleClick={this.handleBubbleClick}
      />
    );
  }

  render() {
    if (this.props.components.length <= 0) {
      return <EmptyResult />;
    }

    const { xMetric, yMetric, sizeMetric } = this.getBubbleMetrics(this.props);
    return (
      <div className="measure-details-bubble-chart">
        <div className="measure-details-bubble-chart-header">
          <span>
            {translateWithParameters(
              'component_measures.domain_x_overview',
              getLocalizedMetricDomain(this.props.domain)
            )}
          </span>
          <span className="measure-details-bubble-chart-legend">
            {translateWithParameters(
              'component_measures.legend.size_x',
              getLocalizedMetricName(sizeMetric)
            )}
          </span>
        </div>
        <div>
          {this.renderBubbleChart(xMetric, yMetric, sizeMetric)}
        </div>
        <div className="measure-details-bubble-chart-axis x">
          {getLocalizedMetricName(xMetric)}
        </div>
        <div className="measure-details-bubble-chart-axis y">
          {getLocalizedMetricName(yMetric)}
        </div>
      </div>
    );
  }
}
