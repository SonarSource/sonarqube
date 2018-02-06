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
import { map } from 'lodash';
import HealthItem from './HealthItem';
import CheckIcon from '../../../../components/icons-components/CheckIcon';
import ClearIcon from '../../../../components/icons-components/ClearIcon';
import { HealthType, SysValue, SysValueObject } from '../../../../api/system';
import { HEALTH_FIELD } from '../../utils';

interface Props {
  name: string;
  value: SysValue;
}

export default function SysInfoItem({ name, value }: Props): JSX.Element {
  if (name === HEALTH_FIELD || name === 'State') {
    return <HealthItem className="no-margin" health={value as HealthType} />;
  }
  if (value instanceof Array) {
    return <code>{JSON.stringify(value)}</code>;
  }
  switch (typeof value) {
    case 'boolean':
      return <BooleanItem value={value as boolean} />;
    case 'object':
      return <ObjectItem value={value as SysValueObject} />;
    default:
      return <code>{value}</code>;
  }
}

function BooleanItem({ value }: { value: boolean }) {
  if (value) {
    return <CheckIcon />;
  } else {
    return <ClearIcon />;
  }
}

function ObjectItem({ value }: { value: SysValueObject }) {
  return (
    <table className="data">
      <tbody>
        {map(value, (value, name) => (
          <tr key={name}>
            <td className="thin nowrap">{name}</td>
            <td>
              <SysInfoItem name={name} value={value} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
