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

import { Avatar, ContentCell, Note, TableRowInteractive } from '~design-system';
import { Image } from '~sonar-aligned/components/common/Image';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { isPermissionDefinitionGroup } from '../../helpers/permissions';
import { isDefined } from '../../helpers/types';
import { PermissionDefinitions, PermissionUser } from '../../types/types';
import PermissionCell from './PermissionCell';
import usePermissionChange from './usePermissionChange';

interface Props {
  isGitHubUser: boolean | undefined;
  isGitLabUser: boolean | undefined;
  onToggle: (user: PermissionUser, permission: string) => Promise<void>;
  permissions: PermissionDefinitions;
  removeOnly?: boolean;
  selectedPermission?: string;
  user: PermissionUser;
}

export default function UserHolder(props: Props) {
  const { user, removeOnly, permissions, selectedPermission, isGitHubUser, isGitLabUser } = props;
  const { loading, handleCheck, modal } = usePermissionChange({
    holder: user,
    onToggle: props.onToggle,
    permissions,
    removeOnly,
  });

  const permissionCells = permissions.map((permission) => (
    <PermissionCell
      key={isPermissionDefinitionGroup(permission) ? permission.category : permission.key}
      loading={loading}
      onCheck={handleCheck}
      permission={permission}
      disabled={isGitHubUser || isGitLabUser}
      removeOnly={removeOnly}
      permissionItem={user}
      prefixID={user.login}
      selectedPermission={selectedPermission}
    />
  ));

  if (user.login === '<creator>') {
    return (
      <TableRowInteractive>
        <ContentCell>
          <div className="sw-max-w-abs-800">
            <div className="sw-flex sw-flex-col sw-w-fit sw-max-w-full">
              <strong className="sw-text-ellipsis sw-whitespace-nowrap sw-overflow-hidden">
                {user.name}
              </strong>
              <p className="sw-mt-2">
                {translate('permission_templates.project_creators.explanation')}
              </p>
            </div>
          </div>
        </ContentCell>
        {permissionCells}
      </TableRowInteractive>
    );
  }

  return (
    <TableRowInteractive>
      <ContentCell>
        <div className="sw-flex sw-items-center">
          <Avatar className="sw-mr-4" hash={user.avatar} name={user.name} size="md" />
          <div className="sw-max-w-abs-800">
            <div className="sw-flex sw-w-fit sw-max-w-full">
              <div className="sw-flex-1 sw-text-ellipsis sw-whitespace-nowrap sw-overflow-hidden">
                <strong>{user.name}</strong>
                <Note className="sw-ml-2">{user.login}</Note>
              </div>
              {isGitHubUser && (
                <Image
                  alt="github"
                  className="sw-ml-2"
                  height={16}
                  aria-label={translateWithParameters(
                    'project_permission.managed',
                    translate('alm.github'),
                  )}
                  src="/images/alm/github.svg"
                />
              )}
              {isGitLabUser && (
                <Image
                  alt="gitlab"
                  className="sw-ml-2"
                  height={16}
                  aria-label={translateWithParameters(
                    'project_permission.managed',
                    translate('alm.gitlab'),
                  )}
                  src="/images/alm/gitlab.svg"
                />
              )}
            </div>
            {isDefined(user.email) && (
              <div className="sw-mt-2 sw-max-w-100 sw-text-ellipsis sw-whitespace-nowrap sw-overflow-hidden">
                {user.email}
              </div>
            )}
          </div>
        </div>
      </ContentCell>
      {permissionCells}
      {modal}
    </TableRowInteractive>
  );
}
