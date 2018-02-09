/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import WebhookActions from './WebhookActions';
import AlertErrorIcon from '../../../components/icons-components/AlertErrorIcon';
import AlertSuccessIcon from '../../../components/icons-components/AlertSuccessIcon';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { Webhook, WebhookDelivery } from '../../../app/types';
import { translate } from '../../../helpers/l10n';

interface Props {
  onDelete: (webhook: string) => Promise<void>;
  onUpdate: (data: { webhook: string; name: string; url: string }) => Promise<void>;
  webhook: Webhook;
}

export default function WebhookItem({ onDelete, onUpdate, webhook }: Props) {
  return (
    <tr>
      <td>{webhook.name}</td>
      <td>{webhook.url}</td>
      <td>
        <LatestDelivery latestDelivery={webhook.latestDelivery} />
      </td>
      <td className="thin nowrap text-right">
        <WebhookActions onDelete={onDelete} onUpdate={onUpdate} webhook={webhook} />
      </td>
    </tr>
  );
}

export function LatestDelivery({ latestDelivery }: { latestDelivery?: WebhookDelivery }) {
  if (!latestDelivery) {
    return <span>{translate('webhooks.last_execution.none')}</span>;
  }
  return (
    <>
      {latestDelivery.success ? <AlertSuccessIcon /> : <AlertErrorIcon />}
      <span className="spacer-left">
        <DateTimeFormatter date={latestDelivery.at} />
      </span>
    </>
  );
}
