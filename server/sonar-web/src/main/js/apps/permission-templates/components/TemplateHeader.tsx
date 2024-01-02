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
import Link from '../../../components/common/Link';
import { translate } from '../../../helpers/l10n';
import { PermissionTemplate } from '../../../types/types';
import { PERMISSION_TEMPLATES_PATH } from '../utils';
import ActionsCell from './ActionsCell';

interface Props {
  loading: boolean;
  refresh: () => void;
  template: PermissionTemplate;
  topQualifiers: string[];
}

export default function TemplateHeader(props: Props) {
  const { template } = props;
  return (
    <header className="page-header" id="project-permissions-header">
      <div className="note spacer-bottom">
        <Link to={PERMISSION_TEMPLATES_PATH}>{translate('permission_templates.page')}</Link>
      </div>

      <h1 className="page-title">{template.name}</h1>

      {props.loading && <i className="spinner" />}

      <div className="pull-right">
        <ActionsCell
          fromDetails={true}
          permissionTemplate={template}
          refresh={props.refresh}
          topQualifiers={props.topQualifiers}
        />
      </div>
    </header>
  );
}
