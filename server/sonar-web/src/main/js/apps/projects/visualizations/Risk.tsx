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
import { Project } from '../types';
import ColorRatingsLegend from '../../../components/charts/ColorRatingsLegend';
import BubbleChart from '../../../components/charts/BubbleChart';
import { formatMeasure } from '../../../helpers/measures';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { RATING_COLORS } from '../../../helpers/constants';
import { getProjectUrl } from '../../../helpers/urls';

const X_METRIC = 'sqale_index';
const X_METRIC_TYPE = 'SHORT_WORK_DUR';
const Y_METRIC = 'coverage';
const Y_METRIC_TYPE = 'PERCENT';
const SIZE_METRIC = 'ncloc';
const SIZE_METRIC_TYPE = 'SHORT_INT';
const COLOR_METRIC_1 = 'reliability_rating';
const COLOR_METRIC_2 = 'security_rating';
const COLOR_METRIC_TYPE = 'RATING';

interface Props {
  displayOrganizations: boolean;
  projects: Project[];
}

export default class Risk extends React.PureComponent<Props> {
  getMetricTooltip(metric: { key: string; type: string }, value?: number) {
    const name = translate('metric', metric.key, 'name');
    const formattedValue = value != null ? formatMeasure(value, metric.type) : '–';
    return `<div>${name}: ${formattedValue}</div>`;
  }

  getTooltip(
    project: Project,
    x?: number,
    y?: number,
    size?: number,
    color1?: number,
    color2?: number
  ) {
    const fullProjectName =
      this.props.displayOrganizations && project.organization
        ? `${project.organization.name} / <strong>${project.name}</strong>`
        : `<strong>${project.name}</strong>`;
    const inner = [
      `<div class="little-spacer-bottom">${fullProjectName}</div>`,
      this.getMetricTooltip({ key: COLOR_METRIC_1, type: COLOR_METRIC_TYPE }, color1),
      this.getMetricTooltip({ key: COLOR_METRIC_2, type: COLOR_METRIC_TYPE }, color2),
      this.getMetricTooltip({ key: Y_METRIC, type: Y_METRIC_TYPE }, y),
      this.getMetricTooltip({ key: X_METRIC, type: X_METRIC_TYPE }, x),
      this.getMetricTooltip({ key: SIZE_METRIC, type: SIZE_METRIC_TYPE }, size)
    ].join('');
    return `<div class="text-left">${inner}</div>`;
  }

  render() {
    const items = this.props.projects.map(project => {
      const x = project.measures[X_METRIC] != null ? Number(project.measures[X_METRIC]) : undefined;
      const y = project.measures[Y_METRIC] != null ? Number(project.measures[Y_METRIC]) : undefined;
      const size =
        project.measures[SIZE_METRIC] != null ? Number(project.measures[SIZE_METRIC]) : undefined;
      const color1 =
        project.measures[COLOR_METRIC_1] != null
          ? Number(project.measures[COLOR_METRIC_1])
          : undefined;
      const color2 =
        project.measures[COLOR_METRIC_2] != null
          ? Number(project.measures[COLOR_METRIC_2])
          : undefined;
      return {
        x: x || 0,
        y: y || 0,
        size: size || 0,
        color:
          color1 != null && color2 != null
            ? RATING_COLORS[Math.max(color1, color2) - 1]
            : undefined,
        key: project.key,
        tooltip: this.getTooltip(project, x, y, size, color1, color2),
        link: getProjectUrl(project.key)
      };
    });

    const formatXTick = (tick: number) => formatMeasure(tick, X_METRIC_TYPE);
    const formatYTick = (tick: number) => formatMeasure(tick, Y_METRIC_TYPE);

    return (
      <div>
        <BubbleChart
          formatXTick={formatXTick}
          formatYTick={formatYTick}
          height={600}
          items={items}
          padding={[80, 20, 60, 100]}
          yDomain={[100, 0]}
        />
        <div className="measure-details-bubble-chart-axis x">
          {translate('metric', X_METRIC, 'name')}
        </div>
        <div className="measure-details-bubble-chart-axis y">
          {translate('metric', Y_METRIC, 'name')}
        </div>
        <div className="measure-details-bubble-chart-axis size">
          <span className="spacer-right">
            {translateWithParameters(
              'component_measures.legend.color_x',
              translate('projects.worse_of_reliablity_and_security')
            )}
          </span>
          {translateWithParameters(
            'component_measures.legend.size_x',
            translate('metric', SIZE_METRIC, 'name')
          )}
          <ColorRatingsLegend className="big-spacer-top" />
        </div>
      </div>
    );
  }
}
