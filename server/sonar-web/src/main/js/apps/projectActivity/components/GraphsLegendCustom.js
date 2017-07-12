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
import React from 'react';
import GraphsLegendItem from './GraphsLegendItem';
import Tooltip from '../../../components/controls/Tooltip';
import { hasDataValues } from '../utils';
import { translate } from '../../../helpers/l10n';
import type { Metric } from '../types';

type Props = {
  metrics: Array<Metric>,
  removeMetric: string => void,
  series: Array<{ name: string, translatedName: string, style: string }>
};

export default function GraphsLegendCustom({ metrics, removeMetric, series }: Props) {
  return (
    <div className="project-activity-graph-legends">
      {series.map(serie => {
        const metric = metrics.find(metric => metric.key === serie.name);
        const hasData = hasDataValues(serie);
        const legendItem = (
          <GraphsLegendItem
            metric={serie.name}
            name={metric && metric.custom ? metric.name : serie.translatedName}
            showWarning={!hasData}
            style={serie.style}
            removeMetric={removeMetric}
          />
        );
        if (!hasData) {
          return (
            <Tooltip
              key={serie.name}
              overlay={translate('project_activity.graphs.custom.metric_no_history')}
              placement="bottom">
              <span className="spacer-left spacer-right">
                {legendItem}
              </span>
            </Tooltip>
          );
        }
        return (
          <span className="spacer-left spacer-right" key={serie.name}>
            {legendItem}
          </span>
        );
      })}
    </div>
  );
}
