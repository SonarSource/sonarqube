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
import ChartLegendIcon from '../icons-components/ChartLegendIcon';

interface Props {
  index: number;
  translatedName: string;
  value: string;
}

export default function PreviewGraphTooltipsContent({ index, translatedName, value }: Props) {
  return (
    <tr className="overview-analysis-graph-tooltip-line">
      <td className="thin">
        <ChartLegendIcon className="little-spacer-right" index={index} />
      </td>
      <td className="overview-analysis-graph-tooltip-value text-right little-spacer-right thin">
        {value}
      </td>
      <td>
        <div className="text-ellipsis overview-analysis-graph-tooltip-description">
          {translatedName}
        </div>
      </td>
    </tr>
  );
}
