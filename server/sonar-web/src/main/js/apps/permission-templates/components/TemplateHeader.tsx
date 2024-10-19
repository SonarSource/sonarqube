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
import { Link, Title } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Organization, PermissionTemplate } from '../../../types/types';
import { PERMISSION_TEMPLATES_PATH } from '../utils';
import ActionsCell from './ActionsCell';

interface Props {
  organization: Organization;
  refresh: () => void;
  template: PermissionTemplate;
  topQualifiers: string[];
}

export default function TemplateHeader(props: Props) {
  const { organization, template } = props;
  return (
    <header className="sw-mb-2 sw-flex sw-justify-between" id="project-permissions-header">
      <div>
        <div className="sw-mb-2">
          <Link to={`/organizations/${organization.kee}/${PERMISSION_TEMPLATES_PATH}`}>{translate('permission_templates.page')}</Link>
        </div>
        <div>
          <Title>{template.name}</Title>
        </div>
        <div>{translate('global_permissions.page.description')}</div>
      </div>
      <div>
        <ActionsCell
          fromDetails
          permissionTemplate={template}
          refresh={props.refresh}
          topQualifiers={props.topQualifiers}
        />
      </div>
    </header>
  );
}
