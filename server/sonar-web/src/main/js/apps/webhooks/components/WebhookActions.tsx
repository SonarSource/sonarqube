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

import { useState } from 'react';

import {
  ButtonIcon,
  ButtonVariety,
  DropdownMenu,
  IconMoreVertical,
} from '@sonarsource/echoes-react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { WebhookResponse, WebhookUpdatePayload } from '../../../types/webhook';
import CreateWebhookForm from './CreateWebhookForm';
import DeleteWebhookForm from './DeleteWebhookForm';
import DeliveriesForm from './DeliveriesForm';

interface Props {
  onDelete: (webhook: string) => Promise<void>;
  onUpdate: (data: WebhookUpdatePayload) => Promise<void>;
  webhook: WebhookResponse;
}

export default function WebhookActions(props: Props) {
  const { onDelete, onUpdate, webhook } = props;

  const [deleting, setDeleting] = useState(false);
  const [deliveries, setDeliveries] = useState(false);
  const [updating, setUpdating] = useState(false);

  function handleUpdate(data: { name: string; secret?: string; url: string }) {
    return onUpdate({ ...data, webhook: webhook.key });
  }

  return (
    <>
      <DropdownMenu.Root
        className="it__webhook-actions"
        id={webhook.key}
        items={
          <>
            <DropdownMenu.ItemButton onClick={() => setUpdating(true)}>
              {translate('update_verb')}
            </DropdownMenu.ItemButton>
            {webhook.latestDelivery && (
              <DropdownMenu.ItemButton
                className="it__webhook-deliveries"
                onClick={() => setDeliveries(true)}
              >
                {translate('webhooks.deliveries.show')}
              </DropdownMenu.ItemButton>
            )}
            <DropdownMenu.ItemButtonDestructive
              className="it__webhook-delete"
              onClick={() => setDeleting(true)}
            >
              {translate('delete')}
            </DropdownMenu.ItemButtonDestructive>
          </>
        }
      >
        <ButtonIcon
          className="it__webhook-actions"
          Icon={IconMoreVertical}
          ariaLabel={translateWithParameters('webhooks.show_actions', webhook.name)}
          variety={ButtonVariety.Default}
        />
      </DropdownMenu.Root>

      {deliveries && <DeliveriesForm onClose={() => setDeliveries(false)} webhook={webhook} />}

      {updating && (
        <CreateWebhookForm
          onClose={() => setUpdating(false)}
          onDone={handleUpdate}
          webhook={webhook}
        />
      )}

      {deleting && (
        <DeleteWebhookForm
          onClose={() => setDeleting(false)}
          onSubmit={() => onDelete(webhook.key)}
          webhook={webhook}
        />
      )}
    </>
  );
}
