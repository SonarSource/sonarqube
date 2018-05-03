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
import PropTypes from 'prop-types';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import InstanceMessage from '../../../components/common/InstanceMessage';

export default class ListHeader extends React.PureComponent {
  static propTypes = {
    organization: PropTypes.object,
    permissions: PropTypes.array.isRequired
  };

  renderTooltip = permission =>
    permission.key === 'user' || permission.key === 'codeviewer' ? (
      <div>
        <InstanceMessage message={translate('projects_role', permission.key, 'desc')} />
        <div className="alert alert-warning spacer-top">
          {translate('projects_role.public_projects_warning')}
        </div>
      </div>
    ) : (
      <InstanceMessage message={translate('projects_role', permission.key, 'desc')} />
    );

  render() {
    const cells = this.props.permissions.map(permission => (
      <th className="permission-column" key={permission.key}>
        <span className="text-middle">{translate('projects_role', permission.key)}</span>
        <HelpTooltip className="spacer-left" overlay={this.renderTooltip(permission)} />
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
