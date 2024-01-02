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
import { FormattedMessage } from 'react-intl';
import { Button } from '../../../components/controls/buttons';
import ConfirmModal from '../../../components/controls/ConfirmModal';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { Group, isUser } from '../../../types/quality-gates';
import { QualityGate } from '../../../types/types';
import { UserBase } from '../../../types/users';
import PermissionItem from './PermissionItem';
import QualityGatePermissionsAddModal from './QualityGatePermissionsAddModal';

export interface QualityGatePermissionsRendererProps {
  groups: Group[];
  loading: boolean;
  onClickAddPermission: () => void;
  onCloseAddPermission: () => void;
  onSubmitAddPermission: (item: UserBase | Group) => void;
  onCloseDeletePermission: () => void;
  onConfirmDeletePermission: (item: UserBase | Group) => void;
  onClickDeletePermission: (item: UserBase | Group) => void;
  permissionToDelete?: UserBase | Group;
  qualityGate: QualityGate;
  showAddModal: boolean;
  submitting: boolean;
  users: UserBase[];
}

export default function QualityGatePermissionsRenderer(props: QualityGatePermissionsRendererProps) {
  const { groups, loading, permissionToDelete, qualityGate, showAddModal, submitting, users } =
    props;

  return (
    <div className="quality-gate-permissions" data-testid="quality-gate-permissions">
      <h3 className="spacer-bottom">{translate('quality_gates.permissions')}</h3>
      <p className="spacer-bottom">{translate('quality_gates.permissions.help')}</p>
      <div>
        <DeferredSpinner loading={loading}>
          <ul>
            {users.map((user) => (
              <li key={user.login}>
                <PermissionItem onClickDelete={props.onClickDeletePermission} item={user} />
              </li>
            ))}
            {groups.map((group) => (
              <li key={group.name}>
                <PermissionItem onClickDelete={props.onClickDeletePermission} item={group} />
              </li>
            ))}
          </ul>
        </DeferredSpinner>
      </div>

      <Button className="big-spacer-top" onClick={props.onClickAddPermission}>
        {translate('quality_gates.permissions.grant')}
      </Button>

      {showAddModal && (
        <QualityGatePermissionsAddModal
          qualityGate={qualityGate}
          onClose={props.onCloseAddPermission}
          onSubmit={props.onSubmitAddPermission}
          submitting={submitting}
        />
      )}

      {permissionToDelete && (
        <ConfirmModal
          header={
            isUser(permissionToDelete)
              ? translate('quality_gates.permissions.remove.user')
              : translate('quality_gates.permissions.remove.group')
          }
          confirmButtonText={translate('remove')}
          isDestructive={true}
          confirmData={permissionToDelete}
          onClose={props.onCloseDeletePermission}
          onConfirm={props.onConfirmDeletePermission}
        >
          <FormattedMessage
            defaultMessage={
              isUser(permissionToDelete)
                ? translate('quality_gates.permissions.remove.user.confirmation')
                : translate('quality_gates.permissions.remove.group.confirmation')
            }
            id="remove.confirmation"
            values={{
              user: <strong>{permissionToDelete.name}</strong>,
            }}
          />
        </ConfirmModal>
      )}
    </div>
  );
}
