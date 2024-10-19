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
import { ContentCell, HelperHintIcon } from 'design-system';
import * as React from 'react';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';

interface Props {
  permission: {
    groupsCount: number;
    key: string;
    usersCount: number;
    withProjectCreator?: boolean;
  };
}

export default function PermissionCell({ permission: p }: Props) {
  return (
    <ContentCell className="sw-px-2">
      <div>
        <ul>
          {p.withProjectCreator && (
            <li className="sw-mb-2">
              {translate('permission_templates.project_creators')}
              <HelpTooltip
                className="sw-ml-2"
                overlay={translate('permission_templates.project_creators.explanation')}
              >
                <HelperHintIcon className="sw-ml-2" />
              </HelpTooltip>
            </li>
          )}
          <li className="sw-mb-2">
            <strong>{p.usersCount}</strong>
            {'  user(s)'}
          </li>
          <li>
            <strong>{p.groupsCount}</strong>
            {' group(s)'}
          </li>
        </ul>
      </div>
    </ContentCell>
  );
}
