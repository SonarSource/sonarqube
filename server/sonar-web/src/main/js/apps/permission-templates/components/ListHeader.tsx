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
import { ContentCell, FlagMessage, HelperHintIcon, TableRow } from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import InstanceMessage from '../../../components/common/InstanceMessage';
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
        <FlagMessage className="sw-mt-2" variant="warning">
          {translate('projects_role.public_projects_warning')}
        </FlagMessage>
      </div>
    ) : (
      <InstanceMessage message={translate('projects_role', permission.key, 'desc')} />
    );
  }

  render() {
    const cells = this.props.permissions.map((permission) => (
      <ContentCell key={permission.key}>
        <span>{translate('projects_role', permission.key)}</span>
        <HelpTooltip overlay={this.renderTooltip(permission)}>
          <HelperHintIcon className="sw-ml-2" />
        </HelpTooltip>
      </ContentCell>
    ));

    return (
      <TableRow>
        <ContentCell>&nbsp;</ContentCell>
        {cells}
        <ContentCell>&nbsp;</ContentCell>
      </TableRow>
    );
  }
}
