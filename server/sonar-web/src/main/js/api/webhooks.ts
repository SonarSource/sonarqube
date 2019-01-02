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
import { getJSON, post, postJSON } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function createWebhook(data: {
  name: string;
  organization: string | undefined;
  project?: string;
  url: string;
}): Promise<{ webhook: T.Webhook }> {
  return postJSON('/api/webhooks/create', data).catch(throwGlobalError);
}

export function deleteWebhook(data: { webhook: string }): Promise<void | Response> {
  return post('/api/webhooks/delete', data).catch(throwGlobalError);
}

export function searchWebhooks(data: {
  organization: string | undefined;
  project?: string;
}): Promise<{ webhooks: T.Webhook[] }> {
  return getJSON('/api/webhooks/list', data).catch(throwGlobalError);
}

export function updateWebhook(data: {
  webhook: string;
  name: string;
  url: string;
}): Promise<void | Response> {
  return post('/api/webhooks/update', data).catch(throwGlobalError);
}

export function searchDeliveries(data: {
  ceTaskId?: string;
  componentKey?: string;
  webhook?: string;
  p?: number;
  ps?: number;
}): Promise<{
  deliveries: T.WebhookDelivery[];
  paging: T.Paging;
}> {
  return getJSON('/api/webhooks/deliveries', data).catch(throwGlobalError);
}

export function getDelivery(data: {
  deliveryId: string;
}): Promise<{ delivery: T.WebhookDelivery & { payload: string } }> {
  return getJSON('/api/webhooks/delivery', data).catch(throwGlobalError);
}
