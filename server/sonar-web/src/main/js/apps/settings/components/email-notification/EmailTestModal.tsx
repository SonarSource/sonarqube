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
import { Button, ButtonVariety, Modal } from '@sonarsource/echoes-react';
import {
  addGlobalErrorMessage,
  addGlobalSuccessMessage,
  FormField,
  InputField,
  InputTextArea,
} from 'design-system';
import React, { FormEvent, useState } from 'react';
import { useIntl } from 'react-intl';
import { isEmail } from 'validator';
import { useCurrentLoginUser } from '../../../../app/components/current-user/CurrentUserContext';
import { translate } from '../../../../helpers/l10n';
import { useSendTestEmailMutation } from '../../../../queries/emails';

const FORM_ID = 'test-email-form';

export default function EmailTestModal() {
  const [isOpen, setIsOpen] = useState(false);
  const currentUser = useCurrentLoginUser();
  const { formatMessage } = useIntl();
  const { isPending, mutate: sendTestEmail } = useSendTestEmailMutation();

  const [message, setMessage] = useState(
    formatMessage({ id: 'email_notification.test.message_text' }),
  );
  const [recipient, setRecipient] = useState(currentUser.email ?? '');
  const [subject, setSubject] = useState(formatMessage({ id: 'email_notification.test.subject' }));

  const handleFormSubmit = (event: FormEvent) => {
    event.preventDefault();
    sendTestEmail(
      { message, recipient, subject },
      {
        onError: () => {
          addGlobalErrorMessage(translate('email_notification.test.failure'));
        },
        onSuccess: () => {
          addGlobalSuccessMessage(translate('email_notification.test.success'));
        },
        onSettled: () => setIsOpen(false),
      },
    );
  };

  const body = (
    <form className="sw-flex-1" id={FORM_ID} onSubmit={handleFormSubmit}>
      <FormField
        htmlFor="test-email-to"
        label={formatMessage({ id: 'email_notification.test.to_address' })}
        required
      >
        <InputField
          disabled={isPending}
          id="test-email-to"
          onChange={(event) => setRecipient(event.target.value)}
          required
          size="full"
          type="email"
          value={recipient}
        />
      </FormField>
      <FormField
        htmlFor="test-email-subject"
        label={formatMessage({ id: 'email_notification.test.subject' })}
      >
        <InputField
          disabled={isPending}
          id="test-email-subject"
          onChange={(event) => setSubject(event.target.value)}
          size="full"
          type="text"
          value={subject}
        />
      </FormField>
      <FormField
        htmlFor="test-email-message"
        label={formatMessage({ id: 'email_notification.test.message' })}
        required
      >
        <InputTextArea
          disabled={isPending}
          id="test-email-message"
          onChange={(event) => setMessage(event.target.value)}
          required
          rows={5}
          size="full"
          value={message}
        />
      </FormField>
    </form>
  );

  return (
    <Modal
      content={body}
      isOpen={isOpen}
      onOpenChange={setIsOpen}
      primaryButton={
        <Button
          isDisabled={!isEmail(recipient) || isPending}
          form={FORM_ID}
          variety={ButtonVariety.Primary}
          type="submit"
        >
          {formatMessage({ id: 'email_notification.test.submit' })}
        </Button>
      }
      secondaryButton={
        <Button onClick={() => setIsOpen(false)} variety={ButtonVariety.Default}>
          {formatMessage({ id: 'cancel' })}
        </Button>
      }
      title={formatMessage({ id: 'email_notification.test.modal_title' })}
    >
      <div className="sw-flex sw-justify-between sw-items-center">
        <span className="sw-typo-lg-semibold">
          {formatMessage({ id: 'email_notification.test.title' })}
        </span>
        <Button onClick={() => setIsOpen(true)} variety={ButtonVariety.Default}>
          {formatMessage({ id: 'email_notification.test.create_test_email' })}
        </Button>
      </div>
    </Modal>
  );
}
