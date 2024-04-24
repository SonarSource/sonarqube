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
import styled from '@emotion/styled';
import {
  BubbleColorVal,
  HelperHintIcon,
  Highlight,
  Link,
  BubbleChart as OriginalBubbleChart,
  themeColor,
} from 'design-system';
import * as React from 'react';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import {
  getLocalizedMetricDomain,
  getLocalizedMetricName,
  translate,
  translateWithParameters,
} from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { isProject, isView } from '../../../types/component';
import { MetricKey } from '../../../types/metrics';
import {
  ComponentMeasureEnhanced,
  ComponentMeasure as ComponentMeasureI,
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
import ColorRatingsLegend from './ColorRatingsLegend';
import EmptyResult from './EmptyResult';

const HEIGHT = 500;

interface Props {
  component: ComponentMeasureI;
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

export default class BubbleChartView extends React.PureComponent<Props, State> {
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
    metrics: { x: Metric; y: Metric; size: Metric; colors?: Metric[] },
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
      <div className="sw-text-left">
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
        const colors = metrics.colors?.map((metric) => this.getMeasureVal(component, metric));
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
          color: (colorRating as BubbleColorVal) ?? 0,
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
        data-testid="bubble-chart"
        formatXTick={formatXTick}
        formatYTick={formatYTick}
        height={HEIGHT}
        items={items}
        onBubbleClick={this.handleBubbleClick}
        padding={[0, 4, 50, 100]}
        yDomain={getBubbleYDomain(this.props.domain)}
        xDomain={xDomain}
      />
    );
  }

  renderChartHeader(domain: string, sizeMetric: Metric, colorsMetric?: Metric[]) {
    const { ratingFilters } = this.state;
    const { paging, component, branchLike, metrics: propsMetrics } = this.props;
    const metrics = getBubbleMetrics(domain, propsMetrics);

    const title = isProjectOverview(domain)
      ? translate('component_measures.overview', domain, 'title')
      : translateWithParameters(
          'component_measures.domain_x_overview',
          getLocalizedMetricDomain(domain),
        );

    return (
      <div className="sw-flex sw-justify-between sw-gap-3">
        <div>
          <div className="sw-flex sw-items-center sw-whitespace-nowrap">
            <Highlight className="it__measure-overview-bubble-chart-title">{title}</Highlight>
            <HelpTooltip className="sw-ml-2" overlay={this.getDescription(domain)}>
              <HelperHintIcon />
            </HelpTooltip>
          </div>

          {paging?.total && paging?.total > BUBBLES_FETCH_LIMIT && (
            <div className="sw-mt-2">
              ({translate('component_measures.legend.only_first_500_files')})
            </div>
          )}
          {(isView(component?.qualifier) || isProject(component?.qualifier)) && (
            <div className="sw-mt-2">
              <Link
                to={getComponentDrilldownUrl({
                  componentKey: component.key,
                  branchLike,
                  metric: isProjectOverview(domain) ? MetricKey.violations : metrics.size.key,
                  listView: true,
                })}
              >
                {translate('component_measures.overview.see_data_as_list')}
              </Link>
            </div>
          )}
        </div>

        <div className="sw-flex sw-flex-col sw-items-end">
          <div className="sw-text-right">
            {colorsMetric && (
              <span className="sw-mr-3">
                <strong className="sw-body-sm-highlight">
                  {translate('component_measures.legend.color')}
                </strong>{' '}
                {colorsMetric.length > 1
                  ? translateWithParameters(
                      'component_measures.legend.worse_of_x_y',
                      ...colorsMetric.map((metric) => getLocalizedMetricName(metric)),
                    )
                  : getLocalizedMetricName(colorsMetric[0])}
              </span>
            )}
            <strong className="sw-body-sm-highlight">
              {translate('component_measures.legend.size')}
            </strong>{' '}
            {getLocalizedMetricName(sizeMetric)}
          </div>
          {colorsMetric && (
            <ColorRatingsLegend
              className="sw-mt-2"
              filters={ratingFilters}
              onRatingClick={this.handleRatingFilterClick}
            />
          )}
        </div>
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
      <BubbleChartWrapper className="sw-relative sw-body-sm">
        {this.renderChartHeader(domain, metrics.size, metrics.colors)}
        {this.renderBubbleChart(metrics)}
        <div className="sw-text-center">{getLocalizedMetricName(metrics.x)}</div>
        <YAxis className="sw-absolute sw-top-1/2 sw-left-3">
          {getLocalizedMetricName(metrics.y)}
        </YAxis>
      </BubbleChartWrapper>
    );
  }
}

const BubbleChartWrapper = styled.div`
  color: ${themeColor('pageContentLight')};
`;

const YAxis = styled.div`
  transform: rotate(-90deg) translateX(-50%);
  transform-origin: left;
`;
