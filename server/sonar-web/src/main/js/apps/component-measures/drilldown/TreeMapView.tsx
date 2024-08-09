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
import { scaleLinear, scaleOrdinal } from 'd3-scale';
import {
  CSSColor,
  Note,
  QualifierIcon,
  ThemeColors,
  ThemeProp,
  TreeMap,
  TreeMapItem,
  themeColor,
  withTheme,
} from 'design-system';
import { isEmpty } from 'lodash';
import * as React from 'react';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import ColorBoxLegend from '../../../components/charts/ColorBoxLegend';
import ColorGradientLegend from '../../../components/charts/ColorGradientLegend';
import { getComponentMeasureUniqueKey } from '../../../helpers/component';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { ComponentMeasureEnhanced, ComponentMeasureIntern, Metric } from '../../../types/types';
import EmptyResult from './EmptyResult';

interface TreeMapViewProps {
  components: ComponentMeasureEnhanced[];
  handleSelect: (component: ComponentMeasureIntern) => void;
  isLegacyMode: boolean;
  metric: Metric;
}

type Props = TreeMapViewProps & ThemeProp;

interface State {
  treemapItems: Array<TreeMapItem<ComponentMeasureIntern>>;
}

const PERCENT_SCALE_DOMAIN = [0, 25, 50, 75, 100];
const RATING_SCALE_DOMAIN = [1, 2, 3, 4];
const LEGACY_RATING_SCALE_DOMAIN = [1, 2, 3, 4, 5];

const HEIGHT = 500;
const NA_COLORS: [ThemeColors, ThemeColors] = ['treeMap.NA1', 'treeMap.NA2'];
const TREEMAP_COLORS: ThemeColors[] = [
  'treeMap.A',
  'treeMap.B',
  'treeMap.C',
  'treeMap.D',
  'treeMap.E',
];
const TREEMAP_LEGACY_COLORS: ThemeColors[] = [
  'treeMap.legacy.A',
  'treeMap.legacy.B',
  'treeMap.legacy.C',
  'treeMap.legacy.D',
  'treeMap.legacy.E',
];

export class TreeMapView extends React.PureComponent<Props, State> {
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

  getTreemapComponents = ({
    components,
    metric,
  }: Props): Array<TreeMapItem<ComponentMeasureIntern>> => {
    const colorScale = this.getColorScale(metric);
    return components
      .map((component) => {
        const colorMeasure = component.measures.find(
          (measure) => measure.metric.key === metric.key,
        );
        const sizeMeasure = component.measures.find((measure) => measure.metric.key !== metric.key);
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
          key: getComponentMeasureUniqueKey(component) ?? '',
          color: isDefined(colorValue) ? (colorScale as Function)(colorValue) : undefined,
          gradient: !isDefined(colorValue) ? this.getNAGradient() : undefined,
          icon: <QualifierIcon fill="pageContent" qualifier={component.qualifier} />,
          label: [component.name, component.branch].filter((s) => !!s).join(' / '),
          size: sizeValue,
          measureValue: colorValue,
          metric,
          tooltip: this.getTooltip({
            colorMetric: metric,
            colorValue,
            component,
            sizeMetric: sizeMeasure.metric,
            sizeValue,
          }),
          sourceData: component,
        };
      })
      .filter(isDefined);
  };

  getNAGradient = () => {
    const { theme } = this.props;
    const [shade1, shade2] = NA_COLORS.map((c) => themeColor(c)({ theme }));

    return `linear-gradient(-45deg, ${shade1} 25%, ${shade2} 25%, ${shade2} 50%, ${shade1} 50%, ${shade1} 75%, ${shade2} 75%, ${shade2} 100%)`;
  };

  getMappedThemeColors = (): string[] => {
    const { theme, isLegacyMode } = this.props;
    return (isLegacyMode ? TREEMAP_LEGACY_COLORS : TREEMAP_COLORS).map((c) =>
      themeColor(c)({ theme }),
    );
  };

  getLevelColorScale = () =>
    scaleOrdinal<string, string>()
      .domain(['ERROR', 'WARN', 'OK', 'NONE'])
      .range(this.getMappedThemeColors());

  getPercentColorScale = (metric: Metric) => {
    const color = scaleLinear<string, string>().domain(PERCENT_SCALE_DOMAIN);
    color.range(
      metric.higherValuesAreBetter
        ? [...this.getMappedThemeColors()].reverse()
        : this.getMappedThemeColors(),
    );
    return color;
  };

  getRatingColorScale = () => {
    const { isLegacyMode } = this.props;
    return scaleLinear<string, string>()
      .domain(isLegacyMode ? LEGACY_RATING_SCALE_DOMAIN : RATING_SCALE_DOMAIN)
      .range(this.getMappedThemeColors());
  };

  getColorScale = (metric: Metric) => {
    if (metric.type === MetricType.Level) {
      return this.getLevelColorScale();
    }
    if (metric.type === MetricType.Rating) {
      return this.getRatingColorScale();
    }
    return this.getPercentColorScale(metric);
  };

  getTooltip = ({
    colorMetric,
    colorValue,
    component,
    sizeMetric,
    sizeValue,
  }: {
    colorMetric: Metric;
    colorValue?: string;
    component: ComponentMeasureEnhanced;
    sizeMetric: Metric;
    sizeValue: number;
  }) => {
    const formatted =
      colorMetric && colorValue !== undefined ? formatMeasure(colorValue, colorMetric.type) : '—';
    return (
      <div className="sw-text-left">
        {[component.name, component.branch].filter((s) => !isEmpty(s)).join(' / ')}
        <br />
        {`${getLocalizedMetricName(sizeMetric)}: ${formatMeasure(sizeValue, sizeMetric.type)}`}
        <br />
        {`${getLocalizedMetricName(colorMetric)}: ${formatted}`}
      </div>
    );
  };

  handleSelect(node: TreeMapItem<ComponentMeasureIntern>) {
    if (node.sourceData) {
      this.props.handleSelect(node.sourceData);
    }
  }

  renderLegend() {
    const { metric, theme } = this.props;
    const colorScale = this.getColorScale(metric);
    if ([MetricType.Level, MetricType.Rating].includes(metric.type as MetricType)) {
      return <ColorBoxLegend colorScale={colorScale} metricType={metric.type} />;
    }
    return (
      <ColorGradientLegend
        showColorNA
        colorScale={colorScale}
        naColors={NA_COLORS.map((c) => themeColor(c)({ theme })) as [CSSColor, CSSColor]}
        height={30}
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
        ? components[0].measures.find((measure) => measure.metric.key !== metric.key)
        : null;
    return (
      <div data-testid="treemap">
        <Note as="div" className="sw-flex sw-items-start sw-mb-2">
          <span>
            <strong className="sw-mr-1">{translate('component_measures.legend.color')}</strong>
            {getLocalizedMetricName(metric)}
          </span>
          <span className="sw-ml-2 sw-flex-1">
            <strong className="sw-mr-1">{translate('component_measures.legend.size')}</strong>
            {translate(
              'metric',
              sizeMeasure?.metric ? sizeMeasure.metric.key : MetricKey.ncloc,
              'name',
            )}
          </span>
          <span>{this.renderLegend()}</span>
        </Note>
        <AutoSizer disableHeight>
          {({ width }) => (
            <TreeMap<ComponentMeasureIntern>
              height={HEIGHT}
              items={treemapItems}
              onRectangleClick={this.handleSelect.bind(this)}
              width={width}
            />
          )}
        </AutoSizer>
      </div>
    );
  }
}

export default withTheme(TreeMapView);
