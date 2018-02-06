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
import AutoSizer from 'react-virtualized/dist/commonjs/AutoSizer';
import { scaleLinear, scaleOrdinal } from 'd3-scale';
import EmptyResult from './EmptyResult';
import * as theme from '../../../app/theme';
import ColorBoxLegend from '../../../components/charts/ColorBoxLegend';
import ColorGradientLegend from '../../../components/charts/ColorGradientLegend';
import QualifierIcon from '../../../components/icons-components/QualifierIcon';
import TreeMap from '../../../components/charts/TreeMap';
import { translate, translateWithParameters, getLocalizedMetricName } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';
import { getProjectUrl } from '../../../helpers/urls';
/*:: import type { Metric } from '../../../store/metrics/actions'; */
/*:: import type { ComponentEnhanced } from '../types'; */
/*:: import type { TreeMapItem } from '../../../components/charts/TreeMap'; */

/*:: type Props = {|
  branch?: string,
  components: Array<ComponentEnhanced>,
  handleSelect: string => void,
  metric: Metric
|}; */

/*:: type State = {
  treemapItems: Array<TreeMapItem>
}; */

const HEIGHT = 500;
const COLORS = [theme.green, theme.lightGreen, theme.yellow, theme.orange, theme.red];
const LEVEL_COLORS = [theme.red, theme.orange, theme.green, theme.gray71];

export default class TreeMapView extends React.PureComponent {
  /*:: props: Props; */
  /*:: state: State; */

  constructor(props /*: Props */) {
    super(props);
    this.state = { treemapItems: this.getTreemapComponents(props) };
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.components !== this.props.components || nextProps.metric !== this.props.metric) {
      this.setState({ treemapItems: this.getTreemapComponents(nextProps) });
    }
  }

  getTreemapComponents = ({ branch, components, metric } /*: Props */) => {
    const colorScale = this.getColorScale(metric);
    return components
      .map(component => {
        const colorMeasure = component.measures.find(measure => measure.metric.key === metric.key);
        const sizeMeasure = component.measures.find(measure => measure.metric.key !== metric.key);
        if (colorMeasure == null || sizeMeasure == null) {
          return null;
        }
        const colorValue = isDiffMetric(colorMeasure.metric.key)
          ? colorMeasure.leak
          : colorMeasure.value;
        const sizeValue = isDiffMetric(sizeMeasure.metric.key)
          ? sizeMeasure.leak
          : sizeMeasure.value;
        if (sizeValue == null) {
          return null;
        }
        return {
          key: component.refKey || component.key,
          size: sizeValue,
          color: colorValue != null ? colorScale(colorValue) : theme.secondFontColor,
          icon: <QualifierIcon color={theme.baseFontColor} qualifier={component.qualifier} />,
          tooltip: this.getTooltip(
            component.name,
            colorMeasure.metric,
            sizeMeasure.metric,
            colorValue,
            sizeValue
          ),
          label: component.name,
          link: getProjectUrl(component.refKey || component.key, branch)
        };
      })
      .filter(Boolean);
  };

  getLevelColorScale = () =>
    scaleOrdinal()
      .domain(['ERROR', 'WARN', 'OK', 'NONE'])
      .range(LEVEL_COLORS);

  getPercentColorScale = (metric /*: Metric */) => {
    const color = scaleLinear().domain([0, 25, 50, 75, 100]);
    color.range(metric.direction === 1 ? COLORS.reverse() : COLORS);
    return color;
  };

  getRatingColorScale = () =>
    scaleLinear()
      .domain([1, 2, 3, 4, 5])
      .range(COLORS);

  getColorScale = (metric /*: Metric */) => {
    if (metric.type === 'LEVEL') {
      return this.getLevelColorScale();
    }
    if (metric.type === 'RATING') {
      return this.getRatingColorScale();
    }
    return this.getPercentColorScale(metric);
  };

  getTooltip = (
    componentName /*: string */,
    colorMetric /*: Metric */,
    sizeMetric /*: Metric */,
    colorValue /*: ?number */,
    sizeValue /*: number */
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

  renderLegend() {
    const { metric } = this.props;
    const colorScale = this.getColorScale(metric);
    if (['LEVEL', 'RATING'].includes(metric.type)) {
      return (
        <ColorBoxLegend
          className="measure-details-treemap-legend color-box-full"
          colorScale={colorScale}
          metricType={metric.type}
        />
      );
    }
    return (
      <ColorGradientLegend
        className="measure-details-treemap-legend"
        colorScale={colorScale}
        colorNA={theme.secondFontColor}
        direction={metric.direction}
        height={20}
        width={200}
      />
    );
  }

  render() {
    const { treemapItems } = this.state;
    if (treemapItems.length <= 0) {
      return <EmptyResult />;
    }
    const { components, metric } = this.props;
    const sizeMeasure =
      components.length > 0
        ? components[0].measures.find(measure => measure.metric.key !== metric.key)
        : null;
    return (
      <div className="measure-details-treemap">
        <ul className="list-inline note spacer-bottom">
          <li>
            {translateWithParameters(
              'component_measures.legend.color_x',
              getLocalizedMetricName(metric)
            )}
          </li>
          <li>
            {translateWithParameters(
              'component_measures.legend.size_x',
              translate(
                'metric',
                sizeMeasure && sizeMeasure.metric ? sizeMeasure.metric.key : 'ncloc',
                'name'
              )
            )}
          </li>
          <li className="pull-right">{this.renderLegend()}</li>
        </ul>
        <AutoSizer disableHeight={true}>
          {({ width }) => (
            <TreeMap
              items={treemapItems}
              onRectangleClick={this.props.handleSelect}
              height={HEIGHT}
              width={width}
            />
          )}
        </AutoSizer>
      </div>
    );
  }
}
