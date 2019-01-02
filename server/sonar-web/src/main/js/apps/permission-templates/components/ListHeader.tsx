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
import * as React from 'react';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import InstanceMessage from '../../../components/common/InstanceMessage';
import { Alert } from '../../../components/ui/Alert';

interface Props {
  organization: T.Organization | undefined;
  permissions: T.Permission[];
}

export default class ListHeader extends React.PureComponent<Props> {
  renderTooltip(permission: T.Permission) {
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
    const cells = this.props.permissions.map(permission => (
      <th className="permission-column" key={permission.key}>
        <div className="permission-column-inner">
          <span className="text-middle">{translate('projects_role', permission.key)}</span>
          <HelpTooltip className="spacer-left" overlay={this.renderTooltip(permission)} />
        </div>
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
