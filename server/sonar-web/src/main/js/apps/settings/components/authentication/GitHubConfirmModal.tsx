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
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import { translate } from '../../../../helpers/l10n';
import { ProvisioningType } from '../../../../types/provisioning';
import { SettingValue } from './hook/useConfiguration';
import { isAllowToSignUpEnabled, isOrganizationListEmpty } from './hook/useGithubConfiguration';

interface GithubAuthenticationProps {
  onConfirm: () => void;
  onClose: () => void;
  values: Record<string, SettingValue>;
  hasGithubProvisioningTypeChange?: boolean;
  provisioningStatus: ProvisioningType;
}

export default function GitHubConfirmModal(props: Readonly<GithubAuthenticationProps>) {
  const { onConfirm, onClose, hasGithubProvisioningTypeChange, provisioningStatus, values } = props;

  const organizationListIsEmpty = isOrganizationListEmpty(values);
  const allowToSignUpEnabled = isAllowToSignUpEnabled(values);

  return (
    <ConfirmModal
      onConfirm={onConfirm}
      header={translate(
        'settings.authentication.github.confirm',
        hasGithubProvisioningTypeChange ? provisioningStatus : 'insecure',
      )}
      onClose={onClose}
      confirmButtonText={translate(
        'settings.authentication.github.provisioning_change.confirm_changes',
      )}
    >
      {hasGithubProvisioningTypeChange &&
        translate('settings.authentication.github.confirm', provisioningStatus, 'description')}
      {(provisioningStatus === ProvisioningType.auto || allowToSignUpEnabled) &&
        organizationListIsEmpty && (
          <FlagMessage className="sw-mt-2" variant="warning">
            {translate('settings.authentication.github.provisioning_change.insecure_config')}
          </FlagMessage>
        )}
    </ConfirmModal>
  );
}
