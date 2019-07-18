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
import * as classNames from 'classnames';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';

interface Props {
  baseComponent?: T.ComponentMeasure;
  canBePinned?: boolean;
  metrics: string[];
  rootComponent: T.ComponentMeasure;
}

const SHORT_NAME_METRICS = [
  'duplicated_lines_density',
  'new_lines',
  'new_coverage',
  'new_duplicated_lines_density'
];

export default function ComponentsHeader({
  baseComponent,
  canBePinned = true,
  metrics,
  rootComponent
}: Props) {
  const isPortfolio = ['VW', 'SVW'].includes(rootComponent.qualifier);
  let columns: string[] = [];
  if (isPortfolio) {
    columns = [
      translate('metric_domain.Releasability'),
      translate('metric_domain.Reliability'),
      translate('portfolio.metric_domain.vulnerabilities'),
      translate('portfolio.metric_domain.security_hotspots'),
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
        <th className="thin nowrap" colSpan={canBePinned ? 2 : 1} />
        <th />
        {baseComponent &&
          columns.map((column, index) => (
            <th
              className={classNames('thin', {
                'code-components-cell': !isPortfolio && index > 0,
                nowrap: !isPortfolio,
                'text-center': isPortfolio && index < columns.length - 1,
                'text-right': !isPortfolio || index === columns.length - 1
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
