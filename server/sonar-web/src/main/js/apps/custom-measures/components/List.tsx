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
import { CustomMeasure } from '../../../app/types';
import ActionsDropdown, {
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';
import { translate } from '../../../helpers/l10n';
import Tooltip from '../../../components/controls/Tooltip';
import { formatMeasure } from '../../../helpers/measures';
import DateFormatter from '../../../components/intl/DateFormatter';

interface Props {
  measures: CustomMeasure[];
  onDelete: (measureId: string) => Promise<void>;
  onEdit: (data: { description: string; id: string; value: string }) => Promise<void>;
}

export default function List({ measures, onDelete, onEdit }: Props) {
  return (
    <div className="boxed-group boxed-group-inner" id="custom-measures-list">
      {measures.length > 0 ? (
        <table className="data zebra zebra-hover">
          <thead>
            <tr>
              <th>{translate('custom_measures.metric')}</th>
              <th>{translate('value')}</th>
              <th>{translate('description')}</th>
              <th>{translate('date')}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {sortBy(measures, measure => measure.metric.name.toLowerCase()).map(measure => (
              <tr data-metric={measure.metric.key} key={measure.id}>
                <td className="nowrap">
                  <div>
                    <span className="js-custom-measure-metric-name">{measure.metric.name}</span>
                    {measure.pending && (
                      <Tooltip overlay={translate('custom_measures.pending_tooltip')}>
                        <span className="js-custom-measure-pending badge badge-warning spacer-left">
                          {translate('custom_measures.pending')}
                        </span>
                      </Tooltip>
                    )}
                  </div>
                  <span className="js-custom-measure-domain note">{measure.metric.domain}</span>
                </td>

                <td className="nowrap">
                  <strong className="js-custom-measure-value">
                    {formatMeasure(measure.value, measure.metric.type)}
                  </strong>
                </td>

                <td>
                  <span className="js-custom-measure-description">{measure.description}</span>
                </td>

                <td>
                  <MeasureDate measure={measure} /> {translate('by_')}{' '}
                  <span className="js-custom-measure-user">{measure.user.name}</span>
                </td>

                <td className="thin nowrap">
                  <ActionsDropdown>
                    <EditButton measure={measure} onEdit={onEdit} />
                    <ActionsDropdownDivider />
                    <DeleteButton measure={measure} onDelete={onDelete} />
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

function MeasureDate({ measure }: { measure: CustomMeasure }) {
  if (measure.updatedAt) {
    return (
      <>
        {translate('updated_on')}{' '}
        <span className="js-custom-measure-created-at">
          <DateFormatter date={measure.updatedAt} />
        </span>
      </>
    );
  } else if (measure.createdAt) {
    return (
      <>
        {translate('created_on')}{' '}
        <span className="js-custom-measure-created-at">
          <DateFormatter date={measure.createdAt} />
        </span>
      </>
    );
  } else {
    return <>{translate('created')}</>;
  }
}
