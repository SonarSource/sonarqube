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
import Tooltip from '../../components/controls/Tooltip';
import { translate } from '../../helpers/l10n';
import { Serie } from '../../types/project-activity';
import GraphsLegendItem from './GraphsLegendItem';
import { hasDataValues } from './utils';

export interface GraphsLegendCustomProps {
  removeMetric: (metric: string) => void;
  series: Serie[];
}

export default function GraphsLegendCustom(props: GraphsLegendCustomProps) {
  const { series } = props;
  return (
    <ul className="activity-graph-legends">
      {series.map((serie, idx) => {
        const hasData = hasDataValues(serie);
        const legendItem = (
          <GraphsLegendItem
            index={idx}
            metric={serie.name}
            name={serie.translatedName}
            removeMetric={props.removeMetric}
            showWarning={!hasData}
          />
        );
        if (!hasData) {
          return (
            <Tooltip
              key={serie.name}
              overlay={translate('project_activity.graphs.custom.metric_no_history')}
            >
              <li
                className="spacer-left spacer-right"
                aria-label={translate('project_activity.graphs.custom.metric_no_history')}
              >
                {legendItem}
              </li>
            </Tooltip>
          );
        }
        return (
          <li className="spacer-left spacer-right" key={serie.name}>
            {legendItem}
          </li>
        );
      })}
    </ul>
  );
}
