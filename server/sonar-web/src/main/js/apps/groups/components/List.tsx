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
import { sortBy } from 'lodash';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Group } from '../../../types/types';
import ListItem from './ListItem';

interface Props {
  groups: Group[];
  onDelete: (group: Group) => void;
  onEdit: (group: Group) => void;
  onEditMembers: () => void;
  showAnyone: boolean;
}

export default function List(props: Props) {
  return (
    <div className="boxed-group boxed-group-inner">
      <table className="data zebra zebra-hover" id="groups-list">
        <thead>
          <tr>
            <th />
            <th className="nowrap width-10" colSpan={2}>
              {translate('members')}
            </th>
            <th className="nowrap">{translate('description')}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {props.showAnyone && (
            <tr className="js-anyone" key="anyone">
              <td className="width-20">
                <strong className="js-group-name">{translate('groups.anyone')}</strong>
                <span className="spacer-left badge badge-error">{translate('deprecated')}</span>
              </td>
              <td className="width-10" colSpan={2} />
              <td className="width-40" colSpan={2}>
                <span className="js-group-description">
                  {translate('user_groups.anyone.description')}
                </span>
              </td>
            </tr>
          )}

          {sortBy(props.groups, (group) => group.name.toLowerCase()).map((group) => (
            <ListItem
              group={group}
              key={group.name}
              onDelete={props.onDelete}
              onEdit={props.onEdit}
              onEditMembers={props.onEditMembers}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}
