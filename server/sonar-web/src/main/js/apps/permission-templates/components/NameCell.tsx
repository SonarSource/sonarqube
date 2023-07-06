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
import * as React from 'react';
import Link from '../../../components/common/Link';
import { queryToSearch } from '../../../helpers/urls';
import { Organization, PermissionTemplate } from '../../../types/types';
import Defaults from './Defaults';

interface Props {
  template: PermissionTemplate;
  organization: Organization;
}

export default function NameCell({ template, organization }: Props) {
  const pathname = `/organizations/${organization.kee}/permission_templates`;

  return (
    <td className="little-padded-left little-padded-right">
      <Link to={{ pathname, search: queryToSearch({ id: template.id }) }}>
        <strong className="js-name">{template.name}</strong>
      </Link>

      {template.defaultFor.length > 0 && (
        <div className="spacer-top js-defaults">
          <Defaults template={template} />
        </div>
      )}

      {!!template.description && (
        <div className="spacer-top js-description">{template.description}</div>
      )}

      {!!template.projectKeyPattern && (
        <div className="spacer-top js-project-key-pattern">
          Project Key Pattern: <code>{template.projectKeyPattern}</code>
        </div>
      )}
    </td>
  );
}
