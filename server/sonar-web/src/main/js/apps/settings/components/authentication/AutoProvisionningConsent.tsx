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

import { Button } from '@sonarsource/echoes-react';
import { Modal } from 'design-system';
import { noop } from 'lodash';
import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { DocLink } from '../../../../helpers/doc-links';
import {
  useSearchGitHubConfigurationsQuery,
  useUpdateGitHubConfigurationMutation,
} from '../../../../queries/dop-translation';
import { ProvisioningType } from '../../../../types/provisioning';

export default function AutoProvisioningConsent() {
  const { formatMessage } = useIntl();
  const { data: list } = useSearchGitHubConfigurationsQuery();
  const gitHubConfiguration = list?.githubConfigurations[0];

  const { mutate: updateConfig } = useUpdateGitHubConfigurationMutation();

  if (gitHubConfiguration?.userConsentRequiredAfterUpgrade !== true) {
    return null;
  }

  const header = formatMessage({
    id: 'settings.authentication.github.confirm_auto_provisioning.header',
  });

  const onClickAutoProvisioning = () => {
    updateConfig({
      id: gitHubConfiguration.id,
      gitHubConfiguration: {
        userConsentRequiredAfterUpgrade: false,
      },
    });
  };

  const onClickJitProvisioning = () => {
    updateConfig({
      id: gitHubConfiguration.id,
      gitHubConfiguration: {
        provisioningType: ProvisioningType.jit,
        userConsentRequiredAfterUpgrade: false,
      },
    });
  };

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
          <Button onClick={onClickAutoProvisioning}>
            <FormattedMessage id="settings.authentication.github.confirm_auto_provisioning.continue" />
          </Button>
        }
        secondaryButton={
          <Button onClick={onClickJitProvisioning}>
            <FormattedMessage id="settings.authentication.github.confirm_auto_provisioning.switch_jit" />
          </Button>
        }
      />
    </Modal>
  );
}
