/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import PermissionItem from './PermissionItem';
import QualityGatePermissionsAddModal from './QualityGatePermissionsAddModal';

export interface QualityGatePermissionsRendererProps {
  loading: boolean;
  onClickAddPermission: () => void;
  onCloseAddPermission: () => void;
  onSubmitAddPermission: (user: T.UserBase) => void;
  onCloseDeletePermission: () => void;
  onConfirmDeletePermission: (user: T.UserBase) => void;
  onClickDeletePermission: (user: T.UserBase) => void;
  qualityGate: T.QualityGate;
  showAddModal: boolean;
  submitting: boolean;
  userPermissionToDelete?: T.UserBase;
  users: T.UserBase[];
}

export default function QualityGatePermissionsRenderer(props: QualityGatePermissionsRendererProps) {
  const { loading, qualityGate, showAddModal, submitting, userPermissionToDelete, users } = props;

  return (
    <div className="quality-gate-permissions">
      <header className="display-flex-center spacer-bottom">
        <h3>{translate('quality_gates.permissions')}</h3>
      </header>
      <p className="spacer-bottom">{translate('quality_gates.permissions.help')}</p>
      <div>
        <DeferredSpinner loading={loading}>
          <ul>
            {users.map(user => (
              <li key={user.login}>
                <PermissionItem onClickDelete={props.onClickDeletePermission} user={user} />
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

      {userPermissionToDelete && (
        <ConfirmModal
          header={translate('users.remove')}
          confirmButtonText={translate('remove')}
          isDestructive={true}
          confirmData={userPermissionToDelete}
          onClose={props.onCloseDeletePermission}
          onConfirm={props.onConfirmDeletePermission}>
          <FormattedMessage
            defaultMessage={translate('users.remove.confirmation')}
            id="users.remove.confirmation"
            values={{
              user: <strong>{userPermissionToDelete.name}</strong>
            }}
          />
        </ConfirmModal>
      )}
    </div>
  );
}
