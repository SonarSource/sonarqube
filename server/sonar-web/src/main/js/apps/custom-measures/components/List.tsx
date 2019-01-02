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
import Item from './Item';
import { translate } from '../../../helpers/l10n';

interface Props {
  measures: T.CustomMeasure[];
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
              <Item key={measure.id} measure={measure} onDelete={onDelete} onEdit={onEdit} />
            ))}
          </tbody>
        </table>
      ) : (
        <p>{translate('no_results')}</p>
      )}
    </div>
  );
}
