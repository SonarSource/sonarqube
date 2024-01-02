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
import * as React from 'react';
import InstanceMessage from '../../../components/common/InstanceMessage';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { Permission } from '../../../types/types';

interface Props {
  permissions: Permission[];
}

export default class ListHeader extends React.PureComponent<Props> {
  renderTooltip(permission: Permission) {
    return permission.key === 'user' || permission.key === 'codeviewer' ? (
      <div>
        <InstanceMessage message={translate('projects_role', permission.key, 'desc')} />
        <Alert className="spacer-top" variant="warning">
          {translate('projects_role.public_projects_warning')}
        </Alert>
      </div>
    ) : (
      <InstanceMessage message={translate('projects_role', permission.key, 'desc')} />
    );
  }

  render() {
    const cells = this.props.permissions.map((permission) => (
      <th className="permission-column little-padded-left little-padded-right" key={permission.key}>
        <div className="permission-column-inner">
          <span className="text-middle">{translate('projects_role', permission.key)}</span>
          <HelpTooltip className="spacer-left" overlay={this.renderTooltip(permission)} />
        </div>
      </th>
    ));

    return (
      <thead>
        <tr>
          <th className="little-padded-left little-padded-right">&nbsp;</th>
          {cells}
          <th className="thin nowrap text-right little-padded-left little-padded-right">&nbsp;</th>
        </tr>
      </thead>
    );
  }
}
