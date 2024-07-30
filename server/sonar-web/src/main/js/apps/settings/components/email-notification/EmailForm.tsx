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
import { Spinner } from '@sonarsource/echoes-react';
import {
  BasicSeparator,
  ButtonPrimary,
  FlagMessage,
  FormField,
  InputField,
  InputTextArea,
  SubHeading,
} from 'design-system';
import React, { ChangeEvent, FormEvent, useState } from 'react';
import { useIntl } from 'react-intl';
import { useCurrentLoginUser } from '../../../../app/components/current-user/CurrentUserContext';
import MandatoryFieldsExplanation from '../../../../components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { parseError } from '../../../../helpers/request';
import { useSendTestEmailMutation } from '../../../../queries/emails';

export default function EmailForm() {
  const currentUser = useCurrentLoginUser();
  const { formatMessage } = useIntl();
  const { isPending, isSuccess, mutate: sendTestEmail } = useSendTestEmailMutation();

  const [error, setError] = useState<string | undefined>();
  const [message, setMessage] = useState(
    formatMessage({ id: 'email_configuration.test.message_text' }),
  );
  const [recipient, setRecipient] = useState(currentUser.email ?? '');
  const [subject, setSubject] = useState(formatMessage({ id: 'email_configuration.test.subject' }));

  const handleFormSubmit = (event: FormEvent) => {
    event.preventDefault();
    sendTestEmail(
      { message, recipient, subject },
      {
        onError: async (error) => {
          const errorMessage = await parseError(error);
          setError(errorMessage);
        },
      },
    );
  };

  const onMessageChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
    setMessage(event.target.value);
  };

  const onRecipientChange = (event: ChangeEvent<HTMLInputElement>) => {
    setRecipient(event.target.value);
  };

  const onSubjectChange = (event: ChangeEvent<HTMLInputElement>) => {
    setSubject(event.target.value);
  };

  return (
    <>
      <BasicSeparator />
      <div className="sw-p-6 sw-flex sw-gap-12">
        <div className="sw-w-abs-300">
          <SubHeading>{translate('email_configuration.test.title')}</SubHeading>
          <div className="sw-mt-1">
            <MandatoryFieldsExplanation />
          </div>
        </div>

        <form className="sw-flex-1" onSubmit={handleFormSubmit}>
          {isSuccess && (
            <FlagMessage variant="success">
              {translateWithParameters('email_configuration.test.email_was_sent_to_x', recipient)}
            </FlagMessage>
          )}

          {error !== undefined && <FlagMessage variant="error">{error}</FlagMessage>}

          <FormField label={translate('email_configuration.test.to_address')} required>
            <InputField
              disabled={isPending}
              id="test-email-to"
              onChange={onRecipientChange}
              required
              size="large"
              type="email"
              value={recipient}
            />
          </FormField>
          <FormField label={translate('email_configuration.test.subject')}>
            <InputField
              disabled={isPending}
              id="test-email-subject"
              onChange={onSubjectChange}
              size="large"
              type="text"
              value={subject}
            />
          </FormField>
          <FormField label={translate('email_configuration.test.message')} required>
            <InputTextArea
              disabled={isPending}
              id="test-email-message"
              onChange={onMessageChange}
              required
              rows={5}
              size="large"
              value={message}
            />
          </FormField>

          <ButtonPrimary disabled={isPending} type="submit" className="sw-mt-2">
            {translate('email_configuration.test.send')}
          </ButtonPrimary>
          <Spinner isLoading={isPending} className="sw-ml-2" />
        </form>
      </div>
    </>
  );
}
