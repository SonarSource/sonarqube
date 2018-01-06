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
import { Link } from 'react-router';
import Defaults from './Defaults';
import { PermissionTemplateType } from '../propTypes';

export default class NameCell extends React.PureComponent {
  static propTypes = {
    organization: PropTypes.object,
    permissionTemplate: PermissionTemplateType.isRequired
  };

  render() {
    const { permissionTemplate: t, organization } = this.props;

    const pathname = organization
      ? `/organizations/${organization.key}/permission_templates`
      : '/permission_templates';

    return (
      <td>
        <Link to={{ pathname, query: { id: t.id } }}>
          <strong className="js-name">{t.name}</strong>
        </Link>

        {t.defaultFor.length > 0 && (
          <div className="spacer-top js-defaults">
            <Defaults
              permissionTemplate={this.props.permissionTemplate}
              organization={organization}
            />
          </div>
        )}

        {!!t.description && <div className="spacer-top js-description">{t.description}</div>}

        {!!t.projectKeyPattern && (
          <div className="spacer-top js-project-key-pattern">
            Project Key Pattern: <code>{t.projectKeyPattern}</code>
          </div>
        )}
      </td>
    );
  }
}
