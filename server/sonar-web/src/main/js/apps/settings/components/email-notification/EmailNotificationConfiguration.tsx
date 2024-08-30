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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { NumberedList, NumberedListItem } from 'design-system/lib';
import React, { useCallback, useEffect } from 'react';
import { translate } from '../../../../helpers/l10n';
import {
  useSaveEmailConfigurationMutation,
  useUpdateEmailConfigurationMutation,
} from '../../../../queries/system';
import { AuthMethod, EmailConfiguration } from '../../../../types/system';
import { AuthenticationSelector } from './AuthenticationSelector';
import { CommonSMTP } from './CommonSMTP';
import { SenderInformation } from './SenderInformation';
import { checkEmailConfigurationValidity } from './utils';

interface Props {
  emailConfiguration: EmailConfiguration | null;
}

const FORM_ID = 'email-notifications';
const EMAIL_CONFIGURATION_DEFAULT: EmailConfiguration = {
  authMethod: AuthMethod.Basic,
  basicPassword: '',
  fromAddress: '',
  fromName: 'SonarQube',
  host: '',
  port: '587',
  securityProtocol: '',
  subjectPrefix: '[SonarQube]',
  username: '',
};

export default function EmailNotificationConfiguration(props: Readonly<Props>) {
  const { emailConfiguration } = props;
  const [canSave, setCanSave] = React.useState(false);

  const [newConfiguration, setNewConfiguration] = React.useState<EmailConfiguration>(
    EMAIL_CONFIGURATION_DEFAULT,
  );

  const { mutateAsync: saveEmailConfiguration } = useSaveEmailConfigurationMutation();
  const { mutateAsync: updateEmailConfiguration } = useUpdateEmailConfigurationMutation();

  const onChange = useCallback(
    (newValue: Partial<EmailConfiguration>) => {
      const newConfig = {
        ...newConfiguration,
        ...newValue,
      };
      setCanSave(checkEmailConfigurationValidity(newConfig as EmailConfiguration));
      setNewConfiguration(newConfig as EmailConfiguration);
    },
    [newConfiguration, setNewConfiguration],
  );

  const onSubmit = useCallback(
    (event: React.SyntheticEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (canSave && newConfiguration) {
        const authConfiguration =
          newConfiguration.authMethod === AuthMethod.OAuth
            ? {
                oauthAuthenticationHost: newConfiguration.oauthAuthenticationHost,
                oauthClientId: newConfiguration.oauthClientId,
                oauthClientSecret: newConfiguration.oauthClientSecret,
                oauthTenant: newConfiguration.oauthTenant,
              }
            : {
                basicPassword: newConfiguration.basicPassword,
              };

        const newEmailConfiguration = {
          ...newConfiguration,
          ...authConfiguration,
        };

        if (newConfiguration?.id === undefined) {
          saveEmailConfiguration(newEmailConfiguration);
        } else {
          newEmailConfiguration.id = newConfiguration.id;
          updateEmailConfiguration({
            emailConfiguration: newEmailConfiguration,
            id: newEmailConfiguration.id,
          });
        }
      }
    },
    [canSave, newConfiguration, saveEmailConfiguration, updateEmailConfiguration],
  );

  useEffect(() => {
    if (emailConfiguration !== null) {
      setNewConfiguration(emailConfiguration);
    }
  }, [emailConfiguration]);

  return (
    <form id={FORM_ID} onSubmit={onSubmit}>
      <NumberedList>
        <NumberedListItem className="sw-pt-6">
          <span className="sw-body-sm-highlight">
            {translate('email_notification.subheading.1')}
          </span>
          <AuthenticationSelector configuration={newConfiguration} onChange={onChange} />
        </NumberedListItem>
        <NumberedListItem className="sw-pt-6">
          <span className="sw-body-sm-highlight">
            {translate('email_notification.subheading.2')}
          </span>
          <CommonSMTP configuration={newConfiguration} onChange={onChange} />
        </NumberedListItem>
        <NumberedListItem className="sw-pt-6">
          <span className="sw-body-sm-highlight">
            {translate('email_notification.subheading.3')}
          </span>
          <SenderInformation configuration={newConfiguration} onChange={onChange} />
        </NumberedListItem>
      </NumberedList>
      <Button
        className="sw-ml-4"
        isDisabled={!canSave}
        type="submit"
        variety={ButtonVariety.Primary}
      >
        {translate('email_notification.form.save_configuration')}
      </Button>
    </form>
  );
}
