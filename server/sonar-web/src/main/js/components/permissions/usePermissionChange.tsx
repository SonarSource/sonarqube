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
import { without } from 'lodash';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../helpers/l10n';
import { isPermissionDefinitionGroup } from '../../helpers/permissions';
import {
  PermissionDefinition,
  PermissionDefinitions,
  PermissionGroup,
  PermissionUser,
} from '../../types/types';
import ConfirmModal from '../controls/ConfirmModal';

interface Props<T extends PermissionGroup | PermissionUser> {
  holder: T;
  onToggle: (holder: T, permission: string) => Promise<void>;
  permissions: PermissionDefinitions;
  removeOnly?: boolean;
}

export default function usePermissionChange<T extends PermissionGroup | PermissionUser>(
  props: Props<T>,
) {
  const { holder, removeOnly, permissions } = props;
  const [loading, setLoading] = React.useState<string[]>([]);
  const [confirmPermission, setConfirmPermission] = React.useState<PermissionDefinition | null>(
    null,
  );

  const stopLoading = (permission: string) => {
    setLoading((prevState) => without(prevState, permission));
  };

  const handleCheck = (_checked: boolean, permissionKey?: string) => {
    if (permissionKey !== undefined) {
      if (removeOnly) {
        const flatPermissions = permissions.reduce<PermissionDefinition[]>(
          (acc, p) => (isPermissionDefinitionGroup(p) ? [...acc, ...p.permissions] : [...acc, p]),
          [],
        );
        setConfirmPermission(flatPermissions.find((p) => p.key === permissionKey) ?? null);
      } else {
        handleChangePermission(permissionKey);
      }
    }
  };

  const handleChangePermission = (permission: string) => {
    setLoading((prevState) => [...prevState, permission]);
    return props.onToggle(holder, permission).then(
      () => stopLoading(permission),
      () => stopLoading(permission),
    );
  };

  const modal = (
    <>
      {confirmPermission && (
        <ConfirmModal
          confirmButtonText={translate('confirm')}
          header={translate('project_permission.remove_only_confirmation_title')}
          isDestructive
          isOpen
          onClose={() => setConfirmPermission(null)}
          onConfirm={() => handleChangePermission(confirmPermission.key)}
        >
          <FormattedMessage
            defaultMessage={translate('project_permission.remove_only_confirmation')}
            id="project_permission.remove_only_confirmation"
            values={{
              permission: <b>{confirmPermission.name}</b>,
              holder: <b>{holder.name}</b>,
            }}
          />
        </ConfirmModal>
      )}
    </>
  );

  return { handleCheck, modal, loading };
}
