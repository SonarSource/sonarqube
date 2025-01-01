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

import { NewCodeLegend } from '~design-system';
import { translate } from '../../helpers/l10n';
import { Serie } from '../../types/project-activity';
import { GraphsLegendItem } from './GraphsLegendItem';

export interface GraphsLegendStaticProps {
  leakPeriodDate?: Date;
  series: Array<Pick<Serie, 'name' | 'translatedName'>>;
}

export default function GraphsLegendStatic({
  series,
  leakPeriodDate,
}: Readonly<GraphsLegendStaticProps>) {
  return (
    <ul className="activity-graph-legends sw-flex sw-justify-center sw-items-center sw-pb-4">
      {series.map((serie, idx) => (
        <li key={serie.name}>
          <GraphsLegendItem
            className="sw-mx-2"
            index={idx}
            metric={serie.name}
            name={serie.translatedName}
          />
        </li>
      ))}
      {leakPeriodDate && (
        <li key={translate('hotspot.filters.period.since_leak_period')}>
          <NewCodeLegend
            className="sw-mr-2"
            text={translate('hotspot.filters.period.since_leak_period')}
          />
        </li>
      )}
    </ul>
  );
}
