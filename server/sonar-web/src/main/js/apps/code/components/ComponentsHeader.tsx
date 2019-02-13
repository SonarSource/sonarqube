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
import * as classNames from 'classnames';
import { translate } from '../../../helpers/l10n';

interface Props {
  baseComponent?: T.ComponentMeasure;
  metrics: string[];
  rootComponent: T.ComponentMeasure;
}

const SHORT_NAME_METRICS = [
  'duplicated_lines_density',
  'new_lines',
  'new_coverage',
  'new_duplicated_lines_density'
];

export default function ComponentsHeader({ baseComponent, metrics, rootComponent }: Props) {
  const isPortfolio = ['VW', 'SVW'].includes(rootComponent.qualifier);
  let columns: string[] = [];
  if (isPortfolio) {
    columns = [
      translate('metric_domain.Releasability'),
      translate('metric_domain.Reliability'),
      translate('metric_domain.Security'),
      translate('metric_domain.Maintainability'),
      translate('metric', 'ncloc', 'name')
    ];
  } else {
    columns = metrics.map(metric =>
      translate('metric', metric, SHORT_NAME_METRICS.includes(metric) ? 'short_name' : 'name')
    );
  }

  return (
    <thead>
      <tr className="code-components-header">
        <th className="thin nowrap" colSpan={2} />
        <th />
        {baseComponent &&
          columns.map((column, index) => (
            <th
              className={classNames('thin', 'nowrap', 'text-right', {
                'code-components-cell': index > 0
              })}
              key={column}>
              {column}
            </th>
          ))}
        <th />
      </tr>
    </thead>
  );
}
