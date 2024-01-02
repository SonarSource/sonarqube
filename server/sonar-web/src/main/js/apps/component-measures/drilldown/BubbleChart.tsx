/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import theme from '../../../app/theme';
import OriginalBubbleChart from '../../../components/charts/BubbleChart';
import ColorRatingsLegend from '../../../components/charts/ColorRatingsLegend';
import Link from '../../../components/common/Link';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { RATING_COLORS } from '../../../helpers/constants';
import {
  getLocalizedMetricDomain,
  getLocalizedMetricName,
  translate,
  translateWithParameters,
} from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { isProject } from '../../../types/component';
import { MetricKey } from '../../../types/metrics';
import {
  ComponentMeasureEnhanced,
  ComponentMeasureIntern,
  Dict,
  Metric,
  Paging,
} from '../../../types/types';
import {
  BUBBLES_FETCH_LIMIT,
  getBubbleMetrics,
  getBubbleYDomain,
  isProjectOverview,
} from '../utils';
import EmptyResult from './EmptyResult';

const HEIGHT = 500;

interface Props {
  componentKey: string;
  components: ComponentMeasureEnhanced[];
  branchLike?: BranchLike;
  domain: string;
  metrics: Dict<Metric>;
  paging?: Paging;
  updateSelected: (component: ComponentMeasureIntern) => void;
}

interface State {
  ratingFilters: { [rating: number]: boolean };
}

export default class BubbleChart extends React.PureComponent<Props, State> {
  state: State = {
    ratingFilters: {},
  };

  getMeasureVal = (component: ComponentMeasureEnhanced, metric: Metric) => {
    const measure = component.measures.find((measure) => measure.metric.key === metric.key);
    if (!measure) {
      return undefined;
    }
    return Number(isDiffMetric(metric.key) ? measure.leak : measure.value);
  };

  getTooltip(
    component: ComponentMeasureEnhanced,
    values: { x: number; y: number; size: number; colors?: Array<number | undefined> },
    metrics: { x: Metric; y: Metric; size: Metric; colors?: Metric[] }
  ) {
    const inner = [
      [component.name, isProject(component.qualifier) ? component.branch : undefined]
        .filter((s) => !!s)
        .join(' / '),
      `${metrics.x.name}: ${formatMeasure(values.x, metrics.x.type)}`,
      `${metrics.y.name}: ${formatMeasure(values.y, metrics.y.type)}`,
      `${metrics.size.name}: ${formatMeasure(values.size, metrics.size.type)}`,
    ].filter((s) => !!s);
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

  handleRatingFilterClick = (selection: number) => {
    this.setState(({ ratingFilters }) => {
      return { ratingFilters: { ...ratingFilters, [selection]: !ratingFilters[selection] } };
    });
  };

  handleBubbleClick = (component: ComponentMeasureEnhanced) => this.props.updateSelected(component);

  getDescription(domain: string) {
    const description = `component_measures.overview.${domain}.description`;
    const translatedDescription = translate(description);
    if (description === translatedDescription) {
      return null;
    }
    return translatedDescription;
  }

  renderBubbleChart(metrics: { x: Metric; y: Metric; size: Metric; colors?: Metric[] }) {
    const { ratingFilters } = this.state;

    const items = this.props.components
      .map((component) => {
        const x = this.getMeasureVal(component, metrics.x);
        const y = this.getMeasureVal(component, metrics.y);
        const size = this.getMeasureVal(component, metrics.size);
        const colors =
          metrics.colors && metrics.colors.map((metric) => this.getMeasureVal(component, metric));
        if ((!x && x !== 0) || (!y && y !== 0) || (!size && size !== 0)) {
          return undefined;
        }

        const colorRating = colors && Math.max(...colors.filter(isDefined));

        // Filter out items that match ratingFilters
        if (colorRating !== undefined && ratingFilters[colorRating]) {
          return undefined;
        }

        return {
          x,
          y,
          size,
          color:
            colorRating !== undefined
              ? RATING_COLORS[colorRating - 1]
              : { fill: theme.colors.primary, stroke: theme.colors.primary },
          data: component,
          tooltip: this.getTooltip(component, { x, y, size, colors }, metrics),
        };
      })
      .filter(isDefined);

    const formatXTick = (tick: string | number | undefined) => formatMeasure(tick, metrics.x.type);
    const formatYTick = (tick: string | number | undefined) => formatMeasure(tick, metrics.y.type);

    let xDomain: [number, number] | undefined;
    if (items.reduce((acc, item) => acc + item.x, 0) === 0) {
      // All items are on the 0 axis. This won't display the grid on the X axis,
      // which can make the graph a little hard to read. Force the display of
      // the X grid.
      xDomain = [0, 100];
    }

    return (
      <OriginalBubbleChart<ComponentMeasureEnhanced>
        formatXTick={formatXTick}
        formatYTick={formatYTick}
        height={HEIGHT}
        items={items}
        onBubbleClick={this.handleBubbleClick}
        padding={[25, 60, 50, 60]}
        yDomain={getBubbleYDomain(this.props.domain)}
        xDomain={xDomain}
      />
    );
  }

  renderChartHeader(domain: string, sizeMetric: Metric, colorsMetric?: Metric[]) {
    const { ratingFilters } = this.state;
    const { paging } = this.props;

    const title = isProjectOverview(domain)
      ? translate('component_measures.overview', domain, 'title')
      : translateWithParameters(
          'component_measures.domain_x_overview',
          getLocalizedMetricDomain(domain)
        );
    return (
      <div className="measure-overview-bubble-chart-header">
        <span className="measure-overview-bubble-chart-title">
          <div className="display-flex-center">
            {title}
            <HelpTooltip className="spacer-left" overlay={this.getDescription(domain)} />
          </div>

          {paging?.total && paging?.total > BUBBLES_FETCH_LIMIT && (
            <div className="note spacer-top">
              ({translate('component_measures.legend.only_first_500_files')})
            </div>
          )}
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
                        ...colorsMetric.map((metric) => getLocalizedMetricName(metric))
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
          {colorsMetric && (
            <ColorRatingsLegend
              className="spacer-top"
              filters={ratingFilters}
              onRatingClick={this.handleRatingFilterClick}
            />
          )}
        </span>
      </div>
    );
  }

  render() {
    if (this.props.components.length <= 0) {
      return <EmptyResult />;
    }
    const { domain, componentKey, branchLike } = this.props;
    const metrics = getBubbleMetrics(domain, this.props.metrics);

    return (
      <div className="measure-overview-bubble-chart">
        {this.renderChartHeader(domain, metrics.size, metrics.colors)}
        <div className="measure-overview-bubble-chart-content">
          <div className="text-center small spacer-top spacer-bottom">
            <Link
              to={getComponentDrilldownUrl({
                componentKey,
                branchLike,
                metric: isProjectOverview(domain) ? MetricKey.violations : metrics.size.key,
                listView: true,
              })}
            >
              {translate('component_measures.overview.see_data_as_list')}
            </Link>
          </div>
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
