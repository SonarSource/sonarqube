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
import { sortBy } from 'lodash';
import DeleteButton from './DeleteButton';
import EditButton from './EditButton';
import { MetricProps } from './Form';
import { Metric } from '../../../app/types';
import ActionsDropdown, {
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';
import { translate } from '../../../helpers/l10n';

interface Props {
  domains?: string[];
  metrics: Metric[];
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
              <tr data-metric={metric.key} key={metric.key}>
                <td className="width-30">
                  <div>
                    <strong className="js-metric-name">{metric.name}</strong>
                    <span className="js-metric-key note little-spacer-left">{metric.key}</span>
                  </div>
                </td>

                <td className="width-20">
                  <span className="js-metric-domain">{metric.domain}</span>
                </td>

                <td className="width-20">
                  <span className="js-metric-type">{translate('metric.type', metric.type)}</span>
                </td>

                <td className="width-20" title={metric.description}>
                  <span className="js-metric-description">{metric.description}</span>
                </td>

                <td className="thin nowrap">
                  <ActionsDropdown>
                    {domains &&
                      types && (
                        <EditButton
                          domains={domains}
                          metric={metric}
                          onEdit={onEdit}
                          types={types}
                        />
                      )}
                    <ActionsDropdownDivider />
                    <DeleteButton metric={metric} onDelete={onDelete} />
                  </ActionsDropdown>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p>{translate('no_results')}</p>
      )}
    </div>
  );
}
