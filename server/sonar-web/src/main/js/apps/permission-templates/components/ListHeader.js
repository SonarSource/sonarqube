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
import React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';

export default class ListHeader extends React.PureComponent {
  static propTypes = {
    organization: React.PropTypes.object,
    permissions: React.PropTypes.array.isRequired
  };

  renderTooltip = permission =>
    (permission.key === 'user' || permission.key === 'codeviewer'
      ? <div>
          {permission.description}
          <div className="alert alert-warning spacer-top">
            {translate('projects_role.public_projects_warning')}
          </div>
        </div>
      : permission.description);

  render() {
    const cells = this.props.permissions.map(permission => (
      <th key={permission.key} className="permission-column">
        {permission.name}
        <Tooltip overlay={this.renderTooltip(permission)}>
          <i className="icon-help little-spacer-left" />
        </Tooltip>
      </th>
    ));

    return (
      <thead>
        <tr>
          <th>&nbsp;</th>
          {cells}
          <th className="thin nowrap text-right">&nbsp;</th>
        </tr>
      </thead>
    );
  }
}
