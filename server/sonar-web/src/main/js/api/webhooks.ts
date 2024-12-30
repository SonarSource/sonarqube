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

import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { post, postJSON } from '../helpers/request';
import { Paging } from '../types/types';
import {
  WebhookCreatePayload,
  WebhookDelivery,
  WebhookResponse,
  WebhookSearchDeliveriesPayload,
  WebhookUpdatePayload,
} from '../types/webhook';

export function createWebhook(data: WebhookCreatePayload): Promise<{ webhook: WebhookResponse }> {
  return postJSON('/api/webhooks/create', data).catch(throwGlobalError);
}

export function deleteWebhook(data: { webhook: string }): Promise<void | Response> {
  return post('/api/webhooks/delete', data).catch(throwGlobalError);
}

export function searchWebhooks(data: {
  organization?: string;
  project?: string;
}): Promise<{ webhooks: WebhookResponse[] }> {
  return getJSON('/api/webhooks/list', data).catch(throwGlobalError);
}

export function updateWebhook(data: WebhookUpdatePayload): Promise<void | Response> {
  return post('/api/webhooks/update', data).catch(throwGlobalError);
}

export function searchDeliveries(data: WebhookSearchDeliveriesPayload): Promise<{
  deliveries: WebhookDelivery[];
  paging: Paging;
}> {
  return getJSON('/api/webhooks/deliveries', data).catch(throwGlobalError);
}

export function getDelivery(data: {
  deliveryId: string;
}): Promise<{ delivery: WebhookDelivery & { payload: string } }> {
  return getJSON('/api/webhooks/delivery', data).catch(throwGlobalError);
}
