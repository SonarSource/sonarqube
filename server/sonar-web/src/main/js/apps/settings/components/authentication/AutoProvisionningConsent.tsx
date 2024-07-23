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
import { useUpdateGitHubConfigurationMutation } from '../../../../queries/dop-translation';
import { useUpdateGitLabConfigurationMutation } from '../../../../queries/identity-provider/gitlab';
import { useGetValueQuery, useResetSettingsMutation } from '../../../../queries/settings';
import { GitHubConfigurationResponse } from '../../../../types/dop-translation';
import { GitlabConfiguration, ProvisioningType } from '../../../../types/provisioning';

const CONSENT_SETTING_KEY = 'sonar.auth.gitlab.userConsentForPermissionProvisioningRequired';

interface Props {
  githubConfiguration?: GitHubConfigurationResponse;
  gitlabConfiguration?: GitlabConfiguration;
}

export default function AutoProvisioningConsent(props: Readonly<Props>) {
  const { formatMessage } = useIntl();
  const { githubConfiguration, gitlabConfiguration } = props;

  const { mutate: updateGithubConfig } = useUpdateGitHubConfigurationMutation();
  const { mutate: updateGitlabConfig } = useUpdateGitLabConfigurationMutation();
  const { data: userConsent } = useGetValueQuery(CONSENT_SETTING_KEY);
  const { mutateAsync: resetSettingValue } = useResetSettingsMutation();

  if (
    (githubConfiguration?.userConsentRequiredAfterUpgrade !== true &&
      gitlabConfiguration === undefined) ||
    (!userConsent && githubConfiguration === undefined)
  ) {
    return null;
  }

  const header = formatMessage({
    id: 'settings.authentication.confirm_auto_provisioning.header',
  });

  const onClickAutoProvisioning = () => {
    if (githubConfiguration) {
      updateGithubConfig({
        id: githubConfiguration.id,
        gitHubConfiguration: {
          userConsentRequiredAfterUpgrade: false,
        },
      });
    }
    if (gitlabConfiguration) {
      resetSettingValue({ keys: [CONSENT_SETTING_KEY] });
    }
  };

  const onClickJitProvisioning = () => {
    if (githubConfiguration) {
      updateGithubConfig({
        id: githubConfiguration.id,
        gitHubConfiguration: {
          provisioningType: ProvisioningType.jit,
          userConsentRequiredAfterUpgrade: false,
        },
      });
    }
    if (gitlabConfiguration) {
      updateGitlabConfig({
        id: gitlabConfiguration.id,
        data: {
          provisioningType: ProvisioningType.jit,
        },
      });
      resetSettingValue({ keys: [CONSENT_SETTING_KEY] });
    }
  };

  return (
    <Modal onClose={noop} closeOnOverlayClick={false} isLarge>
      <Modal.Header title={header} />
      <Modal.Body>
        <FormattedMessage
          tagName="p"
          id="settings.authentication.confirm_auto_provisioning.description1"
        />
        <FormattedMessage
          id="settings.authentication.confirm_auto_provisioning.description2"
          tagName="p"
          values={{
            alm: githubConfiguration
              ? formatMessage({ id: 'alm.github' })
              : formatMessage({ id: 'alm.gitlab' }),
            documentation: (
              <DocumentationLink
                to={githubConfiguration ? DocLink.AlmGitHubAuth : DocLink.AlmGitLabAuth}
              >
                <FormattedMessage id="documentation" />
              </DocumentationLink>
            ),
          }}
        />
        <FormattedMessage
          tagName="p"
          id="settings.authentication.confirm_auto_provisioning.question"
        />
      </Modal.Body>
      <Modal.Footer
        primaryButton={
          <Button onClick={onClickAutoProvisioning}>
            <FormattedMessage id="settings.authentication.confirm_auto_provisioning.continue" />
          </Button>
        }
        secondaryButton={
          <Button onClick={onClickJitProvisioning}>
            <FormattedMessage id="settings.authentication.confirm_auto_provisioning.switch_jit" />
          </Button>
        }
      />
    </Modal>
  );
}
