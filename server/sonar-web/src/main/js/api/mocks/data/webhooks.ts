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
import { cloneDeep } from 'lodash';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockWebhook, mockWebhookDelivery } from '../../../helpers/mocks/webhook';
import { WebhookDelivery, WebhookResponse } from '../../../types/webhook';
import {
  WEBHOOK_GLOBAL_1,
  WEBHOOK_GLOBAL_1_LATEST_DELIVERY_ID,
  WEBHOOK_GLOBAL_2,
  WEBHOOK_PROJECT_1,
  WEBHOOK_PROJECT_1_1,
  WEBHOOK_PROJECT_1_2,
} from './ids';

export const webhookProject = mockComponent({
  key: WEBHOOK_PROJECT_1,
  qualifier: ComponentQualifier.Project,
});

const globalWebhook1Deliveries = [
  mockWebhookDelivery({
    at: '2019-06-24T09:45:52+0200',
    id: WEBHOOK_GLOBAL_1_LATEST_DELIVERY_ID,
  }),
  ...Array.from({ length: 15 }).map((_, i) =>
    mockWebhookDelivery({
      id: `global-webhook-1-delivery-${i}`,
      at: `2019-06-${(23 - i).toString().padStart(2, '0')}T09:45:52+0200`,
      httpStatus: i % 2 === 0 ? 200 : undefined,
      success: i % 2 === 0,
      durationMs: 1000 + i * 100,
    }),
  ),
];

const project1Webhook1Deliveries = [
  mockWebhookDelivery({
    id: 'project-1-delivery-1',
    at: '2019-06-24T09:45:52+0200',
  }),
];

export const deliveries = [...globalWebhook1Deliveries, ...project1Webhook1Deliveries];

export const webhooks: Record<string, Array<WebhookResponse>> = {
  global: [
    mockWebhook({
      key: WEBHOOK_GLOBAL_1,
      name: 'Global webhook 1',
      url: 'https://example.com/1',
      latestDelivery: globalWebhook1Deliveries[0],
    }),
    mockWebhook({
      key: WEBHOOK_GLOBAL_2,
      name: 'Global webhook 2',
      hasSecret: true,
      url: 'https://example.com/2',
    }),
  ],
  [webhookProject.key]: [
    mockWebhook({
      key: WEBHOOK_PROJECT_1_1,
      name: 'Project 1 webhook 1',
      url: 'https://example.com/1',
      latestDelivery: project1Webhook1Deliveries[0],
    }),
    mockWebhook({
      key: WEBHOOK_PROJECT_1_2,
      name: 'Project 1 webhook 2',
      url: 'https://example.com/2',
    }),
  ],
};

export function mockFullWebhookList(project?: string): Array<WebhookResponse> {
  return cloneDeep(webhooks[project ?? 'global'] ?? []);
}

export function mockFullWebhookDeliveries(webhook?: string): Array<WebhookDelivery> {
  return cloneDeep(webhook === WEBHOOK_GLOBAL_1 ? globalWebhook1Deliveries : []);
}
