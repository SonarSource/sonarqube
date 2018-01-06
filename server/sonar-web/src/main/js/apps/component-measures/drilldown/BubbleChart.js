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
import EmptyResult from './EmptyResult';
import OriginalBubbleChart from '../../../components/charts/BubbleChart';
import ColorRatingsLegend from '../../../components/charts/ColorRatingsLegend';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';
import {
  getLocalizedMetricDomain,
  getLocalizedMetricName,
  translate,
  translateWithParameters
} from '../../../helpers/l10n';
import { getBubbleMetrics, getBubbleYDomain, isProjectOverview } from '../utils';
import { RATING_COLORS } from '../../../helpers/constants';
/*:: import type { Component, ComponentEnhanced } from '../types'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */

const HEIGHT = 500;

/*:: type Props = {|
  component: Component,
  components: Array<ComponentEnhanced>,
  domain: string,
  metrics: { [string]: Metric },
  updateSelected: string => void
|}; */

export default class BubbleChart extends React.PureComponent {
  /*:: props: Props; */

  getMeasureVal = (component /*: ComponentEnhanced */, metric /*: Metric */) => {
    const measure = component.measures.find(measure => measure.metric.key === metric.key);
    if (measure) {
      return Number(isDiffMetric(metric.key) ? measure.leak : measure.value);
    }
  };

  getTooltip(
    componentName /*: string */,
    values /*: {
      x: number,
      y: number,
      size: number,
      colors: ?Array<?number>
    }*/,
    metrics /*: {
      x: Metric ,
      y: Metric ,
      size: Metric ,
      colors: ?Array<Metric>
    }*/
  ) {
    const inner = [
      componentName,
      `${metrics.x.name}: ${formatMeasure(values.x, metrics.x.type)}`,
      `${metrics.y.name}: ${formatMeasure(values.y, metrics.y.type)}`,
      `${metrics.size.name}: ${formatMeasure(values.size, metrics.size.type)}`
    ];
    if (values.colors && metrics.colors) {
      metrics.colors.forEach((metric, idx) => {
        // $FlowFixMe colors is always defined at this point
        const colorValue = values.colors[idx];
        if (colorValue || colorValue === 0) {
          inner.push(`${metric.name}: ${formatMeasure(colorValue, metric.type)}`);
        }
      });
    }
    return `<div class="text-left">${inner.join('<br/>')}</div>`;
  }

  handleBubbleClick = (component /*: ComponentEnhanced */) =>
    this.props.updateSelected(component.refKey || component.key);

  renderBubbleChart(
    metrics /*: {
      x: Metric ,
      y: Metric ,
      size: Metric ,
      colors: ?Array<Metric>
    }*/
  ) {
    const items = this.props.components
      .map(component => {
        const x = this.getMeasureVal(component, metrics.x);
        const y = this.getMeasureVal(component, metrics.y);
        const size = this.getMeasureVal(component, metrics.size);
        const colors =
          metrics.colors && metrics.colors.map(metric => this.getMeasureVal(component, metric));
        if ((!x && x !== 0) || (!y && y !== 0) || (!size && size !== 0)) {
          return null;
        }
        return {
          x,
          y,
          size,
          color:
            colors != null ? RATING_COLORS[Math.max(...colors.filter(Boolean)) - 1] : undefined,
          link: component,
          tooltip: this.getTooltip(component.name, { x, y, size, colors }, metrics)
        };
      })
      .filter(Boolean);

    const formatXTick = tick => formatMeasure(tick, metrics.x.type);
    const formatYTick = tick => formatMeasure(tick, metrics.y.type);

    return (
      <OriginalBubbleChart
        items={items}
        height={HEIGHT}
        padding={[25, 60, 50, 60]}
        formatXTick={formatXTick}
        formatYTick={formatYTick}
        onBubbleClick={this.handleBubbleClick}
        yDomain={getBubbleYDomain(this.props.domain)}
      />
    );
  }

  renderChartHeader(
    domain /*: string */,
    sizeMetric /*: Metric */,
    colorsMetric /*: ?Array<Metric> */
  ) {
    const title = isProjectOverview(domain)
      ? translate('component_measures.overview', domain, 'title')
      : translateWithParameters(
          'component_measures.domain_x_overview',
          getLocalizedMetricDomain(domain)
        );
    return (
      <div className="measure-overview-bubble-chart-header">
        <span className="measure-overview-bubble-chart-title">{title}</span>
        <span className="measure-overview-bubble-chart-legend">
          <span className="note">
            {colorsMetric && (
              <span className="spacer-right">
                {translateWithParameters(
                  'component_measures.legend.color_x',
                  colorsMetric.length > 1
                    ? translateWithParameters(
                        'component_measures.legend.worse_of_x_y',
                        ...colorsMetric.map(metric => getLocalizedMetricName(metric))
                      )
                    : getLocalizedMetricName(colorsMetric[0])
                )}
              </span>
            )}
            {translateWithParameters(
              'component_measures.legend.size_x',
              getLocalizedMetricName(sizeMetric)
            )}
          </span>
          {colorsMetric && <ColorRatingsLegend className="spacer-top" />}
        </span>
      </div>
    );
  }

  renderChartFooter(domain /*: string */) {
    const description = `component_measures.overview.${domain}.description`;
    const translatedDescription = translate(description);
    if (description === translatedDescription) {
      return null;
    }
    return <div className="measure-overview-bubble-chart-footer">{translatedDescription}</div>;
  }

  render() {
    if (this.props.components.length <= 0) {
      return <EmptyResult />;
    }
    const { domain } = this.props;
    const metrics = getBubbleMetrics(domain, this.props.metrics);

    return (
      <div className="measure-overview-bubble-chart">
        {this.renderChartHeader(domain, metrics.size, metrics.colors)}
        <div className="measure-overview-bubble-chart-content">
          {this.renderBubbleChart(metrics)}
        </div>
        <div className="measure-overview-bubble-chart-axis x">
          {getLocalizedMetricName(metrics.x)}
        </div>
        <div className="measure-overview-bubble-chart-axis y">
          {getLocalizedMetricName(metrics.y)}
        </div>
        {this.renderChartFooter(domain)}
      </div>
    );
  }
}
