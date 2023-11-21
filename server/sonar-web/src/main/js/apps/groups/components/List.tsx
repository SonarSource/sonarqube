/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { sortBy } from 'lodash';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Group } from '../../../types/types';
import ListItem from './ListItem';

interface Props {
  groups: Group[];
  manageProvider: string | undefined;
}

export default function List(props: Props) {
  const { groups, manageProvider } = props;

  return (
    <div className="boxed-group boxed-group-inner">
      <table className="data zebra zebra-hover" id="groups-list">
        <thead>
          <tr>
            <th id="list-group-name">{translate('user_groups.page.group_header')}</th>
            <th id="list-group-member" className="nowrap width-10">
              {translate('members')}
            </th>
            <th id="list-group-description" className="nowrap">
              {translate('description')}
            </th>
            <th id="list-group-actions">{translate('actions')}</th>
          </tr>
        </thead>
        <tbody>
          {sortBy(groups, (group) => group.name.toLowerCase()).map((group) => (
            <ListItem group={group} key={group.name} manageProvider={manageProvider} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
