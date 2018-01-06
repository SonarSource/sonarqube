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
import React from 'react';
import { PermissionType } from '../propTypes';
import { translate } from '../../../helpers/l10n';

export default class PermissionCell extends React.PureComponent {
  static propTypes = {
    permission: PermissionType.isRequired
  };

  render() {
    const { permission: p } = this.props;

    return (
      <td className="permission-column" data-permission={p.key}>
        <div className="permission-column-inner">
          <ul>
            {p.withProjectCreator && (
              <li className="little-spacer-bottom">
                {translate('permission_templates.project_creators')}
              </li>
            )}
            <li className="little-spacer-bottom">
              <strong>{p.usersCount}</strong>
              {'  user(s)'}
            </li>
            <li>
              <strong>{p.groupsCount}</strong>
              {' group(s)'}
            </li>
          </ul>
        </div>
      </td>
    );
  }
}
