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
import { Link } from 'react-router';
import ActionsCell from './ActionsCell';
import { translate } from '../../../helpers/l10n';

interface Props {
  loading: boolean;
  organization: T.Organization | undefined;
  refresh: () => void;
  template: T.PermissionTemplate;
  topQualifiers: string[];
}

export default function TemplateHeader(props: Props) {
  const { template, organization } = props;

  const pathname = organization
    ? `/organizations/${organization.key}/permission_templates`
    : '/permission_templates';

  return (
    <header className="page-header" id="project-permissions-header">
      <div className="note spacer-bottom">
        <Link className="text-muted" to={pathname}>
          {translate('permission_templates.page')}
        </Link>
      </div>

      <h1 className="page-title">{template.name}</h1>

      {props.loading && <i className="spinner" />}

      <div className="pull-right">
        <ActionsCell
          fromDetails={true}
          organization={organization}
          permissionTemplate={template}
          refresh={props.refresh}
          topQualifiers={props.topQualifiers}
        />
      </div>
    </header>
  );
}
