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
import { FlagMessage } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import { ProvisioningType } from '../../../../types/provisioning';
import { Provider } from '../../../../types/types';

interface Props {
  allowUsersToSignUp?: boolean;
  hasProvisioningTypeChange?: boolean;
  isAllowListEmpty: boolean;
  onClose: VoidFunction;
  onConfirm: VoidFunction;
  provider: Provider.Github | Provider.Gitlab;
  provisioningStatus: ProvisioningType;
}

export default function ConfirmProvisioningModal(props: Readonly<Props>) {
  const {
    allowUsersToSignUp,
    hasProvisioningTypeChange,
    isAllowListEmpty,
    onConfirm,
    onClose,
    provider,
    provisioningStatus,
  } = props;

  const intl = useIntl();

  return (
    <ConfirmModal
      onConfirm={onConfirm}
      header={intl.formatMessage({
        id: `settings.authentication.${provider}.confirm.${hasProvisioningTypeChange ? provisioningStatus : 'insecure'}`,
      })}
      onClose={onClose}
      confirmButtonText={intl.formatMessage({
        id: `settings.authentication.${provider}.provisioning_change.confirm_changes`,
      })}
    >
      {hasProvisioningTypeChange &&
        intl.formatMessage({
          id: `settings.authentication.${provider}.confirm.${provisioningStatus}.description`,
        })}
      {(provisioningStatus === ProvisioningType.auto || allowUsersToSignUp) && isAllowListEmpty && (
        <FlagMessage className="sw-mt-2" variant="warning">
          {intl.formatMessage({
            id: `settings.authentication.${provider}.provisioning_change.insecure_config`,
          })}
        </FlagMessage>
      )}
    </ConfirmModal>
  );
}
