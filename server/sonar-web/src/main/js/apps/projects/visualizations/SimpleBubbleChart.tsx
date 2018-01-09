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
import * as React from 'react';
import ColorRatingsLegend from '../../../components/charts/ColorRatingsLegend';
import BubbleChart from '../../../components/charts/BubbleChart';
import { formatMeasure } from '../../../helpers/measures';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { RATING_COLORS } from '../../../helpers/constants';
import { getProjectUrl } from '../../../helpers/urls';
import { Project } from '../types';

export interface Metric {
  key: string;
  type: string;
}

interface Props {
  colorMetric?: string;
  displayOrganizations: boolean;
  projects: Project[];
  sizeMetric: Metric;
  xMetric: Metric;
  yDomain?: [number, number];
  yMetric: Metric;
}

export default class SimpleBubbleChart extends React.PureComponent<Props> {
  getMetricTooltip(metric: Metric, value?: number) {
    const name = translate('metric', metric.key, 'name');
    const formattedValue = value != null ? formatMeasure(value, metric.type) : '–';
    return `<div>${name}: ${formattedValue}</div>`;
  }

  getTooltip(project: Project, x?: number, y?: number, size?: number, color?: number) {
    const fullProjectName =
      this.props.displayOrganizations && project.organization
        ? `${project.organization.name} / <strong>${project.name}</strong>`
        : `<strong>${project.name}</strong>`;

    const inner = [
      `<div class="little-spacer-bottom">${fullProjectName}</div>`,
      this.getMetricTooltip(this.props.xMetric, x),
      this.getMetricTooltip(this.props.yMetric, y),
      this.getMetricTooltip(this.props.sizeMetric, size)
    ];

    if (color) {
      // if `color` is defined then `this.props.colorMetric` is defined too
      this.getMetricTooltip({ key: this.props.colorMetric!, type: 'RATING' }, color);
    }

    return `<div class="text-left">${inner.join('')}</div>`;
  }

  render() {
    const { xMetric, yMetric, sizeMetric, colorMetric } = this.props;

    const items = this.props.projects
      .filter(project => colorMetric == null || project.measures[colorMetric] !== null)
      .map(project => {
        const x =
          project.measures[xMetric.key] != null ? Number(project.measures[xMetric.key]) : undefined;
        const y =
          project.measures[yMetric.key] != null ? Number(project.measures[yMetric.key]) : undefined;
        const size =
          project.measures[sizeMetric.key] != null
            ? Number(project.measures[sizeMetric.key])
            : undefined;
        const color = colorMetric ? Number(project.measures[colorMetric]) : undefined;
        return {
          x: x || 0,
          y: y || 0,
          size: size || 0,
          color: color ? RATING_COLORS[color - 1] : undefined,
          key: project.key,
          tooltip: this.getTooltip(project, x, y, size, color),
          link: getProjectUrl(project.key)
        };
      });

    const formatXTick = (tick: number) => formatMeasure(tick, xMetric.type);
    const formatYTick = (tick: number) => formatMeasure(tick, yMetric.type);

    return (
      <div>
        <BubbleChart
          formatXTick={formatXTick}
          formatYTick={formatYTick}
          height={600}
          items={items}
          padding={[colorMetric ? 80 : 40, 20, 60, 100]}
          yDomain={this.props.yDomain}
        />
        <div className="measure-details-bubble-chart-axis x">
          {translate('metric', xMetric.key, 'name')}
        </div>
        <div className="measure-details-bubble-chart-axis y">
          {translate('metric', yMetric.key, 'name')}
        </div>
        <div className="measure-details-bubble-chart-axis size">
          {colorMetric != null && (
            <span className="spacer-right">
              {translateWithParameters(
                'component_measures.legend.color_x',
                translate('metric', colorMetric, 'name')
              )}
            </span>
          )}
          {translateWithParameters(
            'component_measures.legend.size_x',
            translate('metric', sizeMetric.key, 'name')
          )}
          {colorMetric != null && <ColorRatingsLegend className="big-spacer-top" />}
        </div>
      </div>
    );
  }
}
