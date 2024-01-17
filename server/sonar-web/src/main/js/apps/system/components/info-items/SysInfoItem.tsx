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
import { ContentCell, Table, TableRow } from 'design-system';
import { map } from 'lodash';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { HealthTypes, SysInfoValue } from '../../../../types/types';
import { HEALTH_FIELD, STATE_FIELD } from '../../utils';
import HealthItem from './HealthItem';

export interface Props {
  name: string;
  value: SysInfoValue;
}

const COLUMNS = [0, 'auto'];

export default function SysInfoItem({ name, value }: Readonly<Props>) {
  if (name === HEALTH_FIELD || name === STATE_FIELD) {
    return <HealthItem health={value as HealthTypes} />;
  }
  if (value instanceof Array) {
    return <span className="sw-code">{JSON.stringify(value)}</span>;
  }
  switch (typeof value) {
    case 'boolean':
      return <>{translate(value ? 'yes' : 'no')}</>;
    case 'object':
      return (
        <Table columnCount={COLUMNS.length} columnWidths={COLUMNS}>
          {map(value, (v, n) => (
            <TableRow key={n}>
              <ContentCell className="sw-whitespace-nowrap">{n}</ContentCell>
              <ContentCell>
                <SysInfoItem name={n} value={v} />
              </ContentCell>
            </TableRow>
          ))}
        </Table>
      );
    default:
      return <span className="sw-code">{value}</span>;
  }
}
