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
import OriginalBubbleChart from 'sonar-ui-common/components/charts/BubbleChart';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import {
  getLocalizedMetricDomain,
  getLocalizedMetricName,
  translate,
  translateWithParameters
} from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { isDefined } from 'sonar-ui-common/helpers/types';
import ColorRatingsLegend from '../../../components/charts/ColorRatingsLegend';
import { RATING_COLORS } from '../../../helpers/constants';
import { isDiffMetric } from '../../../helpers/measures';
import { getBubbleMetrics, getBubbleYDomain, isProjectOverview } from '../utils';
import EmptyResult from './EmptyResult';

const HEIGHT = 500;

interface Props {
  component: T.ComponentMeasure;
  components: T.ComponentMeasureEnhanced[];
  domain: string;
  metrics: T.Dict<T.Metric>;
  updateSelected: (component: string) => void;
}

export default class BubbleChart extends React.PureComponent<Props> {
  getMeasureVal = (component: T.ComponentMeasureEnhanced, metric: T.Metric) => {
    const measure = component.measures.find(measure => measure.metric.key === metric.key);
    if (!measure) {
      return undefined;
    }
    return Number(isDiffMetric(metric.key) ? measure.leak : measure.value);
  };

  getTooltip(
    componentName: string,
    values: { x: number; y: number; size: number; colors?: Array<number | undefined> },
    metrics: { x: T.Metric; y: T.Metric; size: T.Metric; colors?: T.Metric[] }
  ) {
    const inner = [
      componentName,
      `${metrics.x.name}: ${formatMeasure(values.x, metrics.x.type)}`,
      `${metrics.y.name}: ${formatMeasure(values.y, metrics.y.type)}`,
      `${metrics.size.name}: ${formatMeasure(values.size, metrics.size.type)}`
    ];
    const { colors: valuesColors } = values;
    const { colors: metricColors } = metrics;
    if (valuesColors && metricColors) {
      metricColors.forEach((metric, idx) => {
        const colorValue = valuesColors[idx];
        if (colorValue || colorValue === 0) {
          inner.push(`${metric.name}: ${formatMeasure(colorValue, metric.type)}`);
        }
      });
    }
    return (
      <div className="text-left">
        {inner.map((line, index) => (
          <React.Fragment key={index}>
            {line}
            {index < inner.length - 1 && <br />}
          </React.Fragment>
        ))}
      </div>
    );
  }

  handleBubbleClick = (component: T.ComponentMeasureEnhanced) =>
    this.props.updateSelected(component.refKey || component.key);

  getDescription(domain: string) {
    const description = `component_measures.overview.${domain}.description`;
    const translatedDescription = translate(description);
    if (description === translatedDescription) {
      return null;
    }
    return translatedDescription;
  }

  renderBubbleChart(metrics: { x: T.Metric; y: T.Metric; size: T.Metric; colors?: T.Metric[] }) {
    const items = this.props.components
      .map(component => {
        const x = this.getMeasureVal(component, metrics.x);
        const y = this.getMeasureVal(component, metrics.y);
        const size = this.getMeasureVal(component, metrics.size);
        const colors =
          metrics.colors && metrics.colors.map(metric => this.getMeasureVal(component, metric));
        if ((!x && x !== 0) || (!y && y !== 0) || (!size && size !== 0)) {
          return undefined;
        }
        return {
          x,
          y,
          size,
          color:
            colors !== undefined
              ? RATING_COLORS[Math.max(...colors.filter(isDefined)) - 1]
              : undefined,
          data: component,
          tooltip: this.getTooltip(component.name, { x, y, size, colors }, metrics)
        };
      })
      .filter(isDefined);

    const formatXTick = (tick: string | number | undefined) => formatMeasure(tick, metrics.x.type);
    const formatYTick = (tick: string | number | undefined) => formatMeasure(tick, metrics.y.type);

    return (
      <OriginalBubbleChart<T.ComponentMeasureEnhanced>
        formatXTick={formatXTick}
        formatYTick={formatYTick}
        height={HEIGHT}
        items={items}
        onBubbleClick={this.handleBubbleClick}
        padding={[25, 60, 50, 60]}
        yDomain={getBubbleYDomain(this.props.domain)}
      />
    );
  }

  renderChartHeader(domain: string, sizeMetric: T.Metric, colorsMetric?: T.Metric[]) {
    const title = isProjectOverview(domain)
      ? translate('component_measures.overview', domain, 'title')
      : translateWithParameters(
          'component_measures.domain_x_overview',
          getLocalizedMetricDomain(domain)
        );
    return (
      <div className="measure-overview-bubble-chart-header">
        <span className="measure-overview-bubble-chart-title">
          <span className="text-middle">{title}</span>
          <HelpTooltip className="spacer-left" overlay={this.getDescription(domain)} />
        </span>
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
      </div>
    );
  }
}
