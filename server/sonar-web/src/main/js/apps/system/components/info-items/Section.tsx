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
import { ContentCell, SubHeading, Table, TableRow } from '~design-system';
import { SysInfoValueObject } from '../../../../types/types';
import SysInfoItem from './SysInfoItem';

interface Props {
  items: SysInfoValueObject;
  name?: string;
}

const COLUMNS = ['0', 'auto'];

export default function Section({ name, items }: Readonly<Props>) {
  return (
    <div className="it__system-info-section">
      {name !== undefined && <SubHeading>{name}</SubHeading>}
      <Table id={name} columnCount={COLUMNS.length} columnWidths={COLUMNS}>
        {map(items, (value, name) => {
          return (
            <TableRow key={name}>
              <ContentCell className="it__system-info-section-item-name">{name}</ContentCell>
              <ContentCell>
                <span className="sw-break-all">
                  <SysInfoItem name={name} value={value} />
                </span>
              </ContentCell>
            </TableRow>
          );
        })}
      </Table>
    </div>
  );
}
