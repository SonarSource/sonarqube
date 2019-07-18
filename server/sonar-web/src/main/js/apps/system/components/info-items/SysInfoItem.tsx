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
import { map } from 'lodash';
import * as React from 'react';
import AlertErrorIcon from 'sonar-ui-common/components/icons/AlertErrorIcon';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import { HEALTH_FIELD, STATE_FIELD } from '../../utils';
import HealthItem from './HealthItem';

export interface Props {
  name: string;
  value: T.SysInfoValue;
}

export default function SysInfoItem({ name, value }: Props): JSX.Element {
  if (name === HEALTH_FIELD || name === STATE_FIELD) {
    return <HealthItem className="no-margin" health={value as T.HealthType} />;
  }
  if (value instanceof Array) {
    return <code>{JSON.stringify(value)}</code>;
  }
  switch (typeof value) {
    case 'boolean':
      return <BooleanItem value={value} />;
    case 'object':
      return <ObjectItem value={value} />;
    default:
      return <code>{value}</code>;
  }
}

function BooleanItem({ value }: { value: boolean }) {
  if (value) {
    return <AlertSuccessIcon />;
  } else {
    return <AlertErrorIcon />;
  }
}

function ObjectItem({ value }: { value: T.SysInfoValueObject }) {
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
