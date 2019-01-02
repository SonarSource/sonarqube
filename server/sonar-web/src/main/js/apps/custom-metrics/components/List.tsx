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
import { sortBy } from 'lodash';
import { MetricProps } from './Form';
import Item from './Item';
import { translate } from '../../../helpers/l10n';

interface Props {
  domains?: string[];
  metrics: T.Metric[];
  onDelete: (metricKey: string) => Promise<void>;
  onEdit: (data: { id: string } & MetricProps) => Promise<void>;
  types?: string[];
}

export default function List({ domains, metrics, onDelete, onEdit, types }: Props) {
  return (
    <div className="boxed-group boxed-group-inner" id="custom-metrics-list">
      {metrics.length > 0 ? (
        <table className="data zebra zebra-hover">
          <tbody>
            {sortBy(metrics, metric => metric.name.toLowerCase()).map(metric => (
              <Item
                domains={domains}
                key={metric.key}
                metric={metric}
                onDelete={onDelete}
                onEdit={onEdit}
                types={types}
              />
            ))}
          </tbody>
        </table>
      ) : (
        <p>{translate('no_results')}</p>
      )}
    </div>
  );
}
