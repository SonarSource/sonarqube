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
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { hasDataValues, Serie } from '../utils';
import GraphsLegendItem from './GraphsLegendItem';

interface Props {
  removeMetric: (metric: string) => void;
  series: Serie[];
}

export default function GraphsLegendCustom({ removeMetric, series }: Props) {
  return (
    <div className="project-activity-graph-legends">
      {series.map((serie, idx) => {
        const hasData = hasDataValues(serie);
        const legendItem = (
          <GraphsLegendItem
            index={idx}
            metric={serie.name}
            name={serie.translatedName}
            removeMetric={removeMetric}
            showWarning={!hasData}
          />
        );
        if (!hasData) {
          return (
            <Tooltip
              key={serie.name}
              overlay={translate('project_activity.graphs.custom.metric_no_history')}>
              <span className="spacer-left spacer-right">{legendItem}</span>
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
