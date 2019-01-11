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
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { scaleLinear, scaleOrdinal } from 'd3-scale';
import EmptyResult from './EmptyResult';
import * as theme from '../../../app/theme';
import ColorBoxLegend from '../../../components/charts/ColorBoxLegend';
import ColorGradientLegend from '../../../components/charts/ColorGradientLegend';
import QualifierIcon from '../../../components/icons-components/QualifierIcon';
import TreeMap, { TreeMapItem } from '../../../components/charts/TreeMap';
import { translate, translateWithParameters, getLocalizedMetricName } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';

interface Props {
  branchLike?: T.BranchLike;
  components: T.ComponentMeasureEnhanced[];
  handleSelect: (component: string) => void;
  metric: T.Metric;
}

interface State {
  treemapItems: TreeMapItem[];
}

const HEIGHT = 500;
const COLORS = [theme.green, theme.lightGreen, theme.yellow, theme.orange, theme.red];
const LEVEL_COLORS = [theme.red, theme.orange, theme.green, theme.gray71];

export default class TreeMapView extends React.PureComponent<Props, State> {
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { treemapItems: this.getTreemapComponents(props) };
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.components !== this.props.components || prevProps.metric !== this.props.metric) {
      this.setState({ treemapItems: this.getTreemapComponents(this.props) });
    }
  }

  getTreemapComponents = ({ components, metric }: Props) => {
    const colorScale = this.getColorScale(metric);
    return components
      .map(component => {
        const colorMeasure = component.measures.find(measure => measure.metric.key === metric.key);
        const sizeMeasure = component.measures.find(measure => measure.metric.key !== metric.key);
        if (!sizeMeasure) {
          return undefined;
        }
        const colorValue =
          colorMeasure && (isDiffMetric(metric.key) ? colorMeasure.leak : colorMeasure.value);
        const rawSizeValue = isDiffMetric(sizeMeasure.metric.key)
          ? sizeMeasure.leak
          : sizeMeasure.value;
        if (rawSizeValue === undefined) {
          return undefined;
        }

        const sizeValue = Number(rawSizeValue);
        if (sizeValue < 1) {
          return undefined;
        }
        return {
          color:
            colorValue !== undefined ? (colorScale as Function)(colorValue) : theme.secondFontColor,
          icon: <QualifierIcon fill={theme.baseFontColor} qualifier={component.qualifier} />,
          key: component.refKey || component.key,
          label: component.name,
          size: sizeValue,
          tooltip: this.getTooltip({
            colorMetric: metric,
            colorValue,
            componentName: component.name,
            sizeMetric: sizeMeasure.metric,
            sizeValue
          })
        };
      })
      .filter(isDefined);
  };

  getLevelColorScale = () =>
    scaleOrdinal<string, string>()
      .domain(['ERROR', 'WARN', 'OK', 'NONE'])
      .range(LEVEL_COLORS);

  getPercentColorScale = (metric: T.Metric) => {
    const color = scaleLinear<string, string>().domain([0, 25, 50, 75, 100]);
    color.range(metric.higherValuesAreBetter ? [...COLORS].reverse() : COLORS);
    return color;
  };

  getRatingColorScale = () =>
    scaleLinear<string, string>()
      .domain([1, 2, 3, 4, 5])
      .range(COLORS);

  getColorScale = (metric: T.Metric) => {
    if (metric.type === 'LEVEL') {
      return this.getLevelColorScale();
    }
    if (metric.type === 'RATING') {
      return this.getRatingColorScale();
    }
    return this.getPercentColorScale(metric);
  };

  getTooltip = ({
    colorMetric,
    colorValue,
    componentName,
    sizeMetric,
    sizeValue
  }: {
    colorMetric: T.Metric;
    colorValue?: string;
    componentName: string;
    sizeMetric: T.Metric;
    sizeValue: number;
  }) => {
    const formatted =
      colorMetric && colorValue !== undefined ? formatMeasure(colorValue, colorMetric.type) : '—';
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
        colorNA={theme.secondFontColor}
        colorScale={colorScale}
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
              height={HEIGHT}
              items={treemapItems}
              onRectangleClick={this.props.handleSelect}
              width={width}
            />
          )}
        </AutoSizer>
      </div>
    );
  }
}
