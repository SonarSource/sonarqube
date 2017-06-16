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
import classNames from 'classnames';
import ChartLegendIcon from '../../../components/icons-components/ChartLegendIcon';

type Props = {
  series: Array<{ name: string, translatedName: string, style: string }>
};

export default function StaticGraphsLegend({ series }: Props) {
  return (
    <div className="project-activity-graph-legends">
      {series.map(serie => (
        <span className="big-spacer-left big-spacer-right" key={serie.name}>
          <ChartLegendIcon
            className={classNames(
              'spacer-right line-chart-legend',
              'line-chart-legend-' + serie.style
            )}
          />
          {serie.translatedName}
        </span>
      ))}
    </div>
  );
}
