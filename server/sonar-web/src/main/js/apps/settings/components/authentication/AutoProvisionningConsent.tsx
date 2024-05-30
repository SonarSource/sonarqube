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

import { ButtonSecondary, Modal } from 'design-system';
import { noop } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { DocLink } from '../../../../helpers/doc-links';
import { translate } from '../../../../helpers/l10n';
import { useToggleGithubProvisioningMutation } from '../../../../queries/identity-provider/github';
import { useGetValueQuery, useResetSettingsMutation } from '../../../../queries/settings';

const GITHUB_PERMISSION_USER_CONSENT =
  'sonar.auth.github.userConsentForPermissionProvisioningRequired';

export default function AutoProvisioningConsent() {
  const toggleGithubProvisioning = useToggleGithubProvisioningMutation();
  const resetSettingsMutation = useResetSettingsMutation();
  const { data } = useGetValueQuery(GITHUB_PERMISSION_USER_CONSENT);

  const header = translate('settings.authentication.github.confirm_auto_provisioning.header');

  const removeConsentFlag = () => {
    resetSettingsMutation.mutate({ keys: [GITHUB_PERMISSION_USER_CONSENT] });
  };

  const switchToJIT = async () => {
    await toggleGithubProvisioning.mutateAsync(false);
    removeConsentFlag();
  };

  const continueWithAuto = async () => {
    await toggleGithubProvisioning.mutateAsync(true);
    removeConsentFlag();
  };

  if (data?.value !== '') {
    return null;
  }

  return (
    <Modal onClose={noop} closeOnOverlayClick={false} isLarge>
      <Modal.Header title={header} />
      <Modal.Body>
        <FormattedMessage
          tagName="p"
          id="settings.authentication.github.confirm_auto_provisioning.description1"
        />
        <FormattedMessage
          id="settings.authentication.github.confirm_auto_provisioning.description2"
          tagName="p"
          values={{
            documentation: (
              <DocumentationLink to={DocLink.AlmGitHubAuth}>
                <FormattedMessage id="documentation" />
              </DocumentationLink>
            ),
          }}
        />
        <FormattedMessage
          tagName="p"
          id="settings.authentication.github.confirm_auto_provisioning.question"
        />
      </Modal.Body>
      <Modal.Footer
        primaryButton={
          <ButtonSecondary onClick={continueWithAuto}>
            {translate('settings.authentication.github.confirm_auto_provisioning.continue')}
          </ButtonSecondary>
        }
        secondaryButton={
          <ButtonSecondary onClick={switchToJIT}>
            {translate('settings.authentication.github.confirm_auto_provisioning.switch_jit')}
          </ButtonSecondary>
        }
      />
    </Modal>
  );
}
