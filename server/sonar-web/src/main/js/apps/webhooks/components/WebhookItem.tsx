/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import WebhookActions from './WebhookActions';
import WebhookItemLatestDelivery from './WebhookItemLatestDelivery';

interface Props {
  onDelete: (webhook: string) => Promise<void>;
  onUpdate: (data: { webhook: string; name: string; url: string }) => Promise<void>;
  webhook: T.Webhook;
}

export default function WebhookItem({ onDelete, onUpdate, webhook }: Props) {
  return (
    <tr>
      <td>{webhook.name}</td>
      <td>{webhook.url}</td>
      <td>{webhook.secret ? translate('yes') : translate('no')}</td>
      <td>
        <WebhookItemLatestDelivery webhook={webhook} />
      </td>
      <td className="thin nowrap text-right">
        <WebhookActions onDelete={onDelete} onUpdate={onUpdate} webhook={webhook} />
      </td>
    </tr>
  );
}
