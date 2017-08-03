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
import { AutoSizer } from 'react-virtualized';
import { scaleLinear, scaleOrdinal } from 'd3-scale';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import TreeMap from '../../../components/charts/TreeMap';
import { translate, translateWithParameters, getLocalizedMetricName } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';
import { getComponentUrl } from '../../../helpers/urls';
import type { Metric } from '../../../store/metrics/actions';
import type { ComponentEnhanced } from '../types';
import type { TreeMapItem } from '../../../components/charts/TreeMap';

type Props = {|
  components: Array<ComponentEnhanced>,
  handleSelect: string => void,
  metric: Metric
|};

type State = {
  treemapItems: Array<TreeMapItem>
};

const HEIGHT = 500;

export default class TreeMapView extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { treemapItems: this.getTreemapComponents(props) };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.components !== this.props.components) {
      this.setState({ treemapItems: this.getTreemapComponents(nextProps) });
    }
  }

  getTreemapComponents = ({ components, metric }: Props): Array<TreeMapItem> => {
    const colorScale = this.getColorScale(metric);
    return components
      .map(component => {
        const colorMeasure = component.measures.find(measure => measure.metric.key === metric.key);
        const sizeMeasure = component.measures.find(measure => measure.metric.key !== metric.key);
        if (colorMeasure == null || sizeMeasure == null) {
          // $FlowFixMe Null values are filtered just after
          return null;
        }
        const colorValue = isDiffMetric(colorMeasure.metric.key)
          ? colorMeasure.leak
          : colorMeasure.value;
        const sizeValue = isDiffMetric(sizeMeasure.metric.key)
          ? sizeMeasure.leak
          : sizeMeasure.value;
        if (sizeValue == null) {
          // $FlowFixMe Null values are filtered just after
          return null;
        }
        return {
          key: component.key,
          size: sizeValue,
          color: colorValue != null ? colorScale(colorValue) : '#777',
          icon: <QualifierIcon qualifier={component.qualifier} />,
          tooltip: this.getTooltip(
            component.name,
            colorMeasure.metric,
            sizeMeasure.metric,
            colorValue,
            sizeValue
          ),
          label: component.name,
          link: getComponentUrl(component.key)
        };
      })
      .filter(component => component != null);
  };

  getLevelColorScale = () =>
    scaleOrdinal()
      .domain(['ERROR', 'WARN', 'OK', 'NONE'])
      .range(['#d4333f', '#ed7d20', '#00aa00', '#b4b4b4']);

  getPercentColorScale = (metric: Metric) => {
    const color = scaleLinear().domain([0, 25, 50, 75, 100]);
    color.range(
      metric.direction === 1
        ? ['#d4333f', '#ed7d20', '#eabe06', '#b0d513', '#00aa00']
        : ['#00aa00', '#b0d513', '#eabe06', '#ed7d20', '#d4333f']
    );
    return color;
  };

  getRatingColorScale = () =>
    scaleLinear()
      .domain([1, 2, 3, 4, 5])
      .range(['#00aa00', '#b0d513', '#eabe06', '#ed7d20', '#d4333f']);

  getColorScale = (metric: Metric) => {
    if (metric.type === 'LEVEL') {
      return this.getLevelColorScale();
    }
    if (metric.type === 'RATING') {
      return this.getRatingColorScale();
    }
    return this.getPercentColorScale(metric);
  };

  getTooltip = (
    componentName: string,
    colorMetric: Metric,
    sizeMetric: Metric,
    colorValue: ?number,
    sizeValue: number
  ) => {
    const formatted =
      colorMetric != null && colorValue != null ? formatMeasure(colorValue, colorMetric.type) : '—';
    return (
      <div className="text-left">
        {componentName}
        <br />
        {`${getLocalizedMetricName(sizeMetric)}: ${formatMeasure(sizeValue, sizeMetric.type)}`}
        <br />
        {`${getLocalizedMetricName(colorMetric)}: ${formatted}`}
      </div>
    );
  };

  render() {
    return (
      <div className="measure-details-treemap">
        <ul className="list-inline note spacer-bottom">
          <li>
            {translateWithParameters(
              'component_measures.legend.color_x',
              getLocalizedMetricName(this.props.metric)
            )}
          </li>
          <li>
            {translateWithParameters(
              'component_measures.legend.size_x',
              translate('metric.ncloc.name')
            )}
          </li>
        </ul>
        <AutoSizer>
          {({ width }) =>
            <TreeMap
              items={this.state.treemapItems}
              onRectangleClick={this.props.handleSelect}
              height={HEIGHT}
              width={width}
            />}
        </AutoSizer>
      </div>
    );
  }
}
