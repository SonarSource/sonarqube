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

export default function SysInfoItem({ name, value }: Props) {
  if (name === HEALTH_FIELD || name === STATE_FIELD) {
    return <HealthItem className="no-margin" health={value as HealthTypes} />;
  }
  if (value instanceof Array) {
    return <code>{JSON.stringify(value)}</code>;
  }
  switch (typeof value) {
    case 'boolean':
      return <>{translate(value ? 'yes' : 'no')}</>;
    case 'object':
      return (
        <table className="data">
          <tbody>
            {map(value, (v, n) => (
              <tr key={n}>
                <td className="thin nowrap">{n}</td>
                <td>
                  <SysInfoItem name={n} value={v} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      );
    default:
      return <code>{value}</code>;
  }
}
