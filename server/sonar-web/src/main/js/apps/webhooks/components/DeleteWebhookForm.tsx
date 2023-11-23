/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { DangerButtonPrimary, Modal } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { WebhookResponse } from '../../../types/webhook';

interface Props {
  onClose: () => void;
  onSubmit: () => Promise<void>;
  webhook: WebhookResponse;
}

const FORM_ID = 'delete-webhook-modal';

export default function DeleteWebhookForm({ onClose, onSubmit, webhook }: Props) {
  const header = translate('webhooks.delete');

  const onFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSubmit();
  };

  const renderForm = (
    <form id={FORM_ID} onSubmit={onFormSubmit}>
      {translateWithParameters('webhooks.delete.confirm', webhook.name)}
    </form>
  );

  return (
    <Modal
      onClose={onClose}
      headerTitle={header}
      isOverflowVisible
      body={renderForm}
      primaryButton={
        <DangerButtonPrimary form={FORM_ID} type="submit">
          {translate('delete')}
        </DangerButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
