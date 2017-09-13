/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import ValueItem from './ValueItem';
import { SysValueObject } from '../../types';

interface Props {
  value: SysValueObject;
}

const ObjectItem: React.StatelessComponent<Props> = ({ value }) => {
  return (
    <table className="data">
      <tbody>
        {map(value, (value, name) => (
          <tr key={name}>
            <td className="thin nowrap">{name}</td>
            <td>
              <ValueItem name={name} value={value} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default ObjectItem;
