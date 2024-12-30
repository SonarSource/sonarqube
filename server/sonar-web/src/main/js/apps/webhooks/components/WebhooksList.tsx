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

import { sortBy } from 'lodash';
import { ActionCell, ContentCell, Table, TableRow } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { WebhookResponse, WebhookUpdatePayload } from '../../../types/webhook';
import WebhookItem from './WebhookItem';

interface Props {
  onDelete: (webhook: string) => Promise<void>;
  onUpdate: (data: WebhookUpdatePayload) => Promise<void>;
  webhooks: WebhookResponse[];
}

const COLUMN_WIDTHS = ['auto', 'auto', 'auto', 'auto', '5%'];

export default function WebhooksList({ webhooks, onDelete, onUpdate }: Props) {
  if (webhooks.length < 1) {
    return <p className="it__webhook-empty-list">{translate('webhooks.no_result')}</p>;
  }

  const tableHeader = (
    <TableRow>
      <ContentCell>{translate('name')}</ContentCell>
      <ContentCell>{translate('webhooks.url')}</ContentCell>
      <ContentCell>{translate('webhooks.secret_header')}</ContentCell>
      <ContentCell>{translate('webhooks.last_execution')}</ContentCell>
      <ActionCell>{translate('actions')}</ActionCell>
    </TableRow>
  );

  return (
    <Table
      className="it__webhooks-list"
      noHeaderTopBorder
      columnCount={COLUMN_WIDTHS.length}
      columnWidths={COLUMN_WIDTHS}
      header={tableHeader}
    >
      {sortBy(webhooks, (webhook) => webhook.name.toLowerCase()).map((webhook) => (
        <WebhookItem key={webhook.key} onDelete={onDelete} onUpdate={onUpdate} webhook={webhook} />
      ))}
    </Table>
  );
}
