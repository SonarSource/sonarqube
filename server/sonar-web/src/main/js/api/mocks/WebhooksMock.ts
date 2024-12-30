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

import {
  createWebhook,
  deleteWebhook,
  getDelivery,
  searchDeliveries,
  searchWebhooks,
  updateWebhook,
} from '../../api/webhooks';
import { mockWebhook } from '../../helpers/mocks/webhook';
import { mockPaging } from '../../helpers/testMocks';
import {
  WebhookCreatePayload,
  WebhookResponse,
  WebhookSearchDeliveriesPayload,
  WebhookUpdatePayload,
} from '../../types/webhook';
import { deliveries, mockFullWebhookDeliveries, mockFullWebhookList } from './data/webhooks';

jest.mock('../../api/webhooks');

export default class WebhooksMock {
  static readonly DEFAULT_PAGE_SIZE = 10;

  project?: string;
  webhooks: Array<WebhookResponse>;

  constructor(project?: string) {
    this.project = project;
    this.webhooks = mockFullWebhookList(project);

    jest.mocked(createWebhook).mockImplementation(this.handleCreateWebhook);
    jest.mocked(searchWebhooks).mockImplementation(this.handleSearchWebhooks);
    jest.mocked(updateWebhook).mockImplementation(this.handleUpdateWebhook);
    jest.mocked(deleteWebhook).mockImplementation(this.handleDeleteWebhook);
    jest.mocked(searchDeliveries).mockImplementation(this.handleSearchDeliveries);
    jest.mocked(getDelivery).mockImplementation(this.handleGetDelivery);
  }

  reset = (project?: string) => {
    this.project = project;
    this.webhooks = mockFullWebhookList(project);
  };

  response = <T>(data: T): Promise<T> => {
    return Promise.resolve(data);
  };

  addWebhook = (...webhooks: WebhookResponse[]) => {
    this.webhooks.push(...webhooks);
  };

  handleCreateWebhook = (data: WebhookCreatePayload) => {
    const webhook = mockWebhook({
      name: data.name,
      url: data.url,
      hasSecret: Boolean(data.secret),
    });
    return this.response({
      webhook,
    });
  };

  handleSearchWebhooks = ({ project }: { project?: string }) => {
    if (project !== this.project) {
      throw new Error(
        'You are asking for webhooks of a project that is not mocked. Reset first the mock with the correct project',
      );
    }
    return this.response({
      webhooks: this.webhooks,
    });
  };

  handleUpdateWebhook = (data: WebhookUpdatePayload) => {
    const webhook = this.webhooks.find((webhook) => webhook.key === data.webhook);
    if (!webhook) {
      return Promise.reject(new Error('Webhook not found'));
    }
    webhook.hasSecret = Boolean(data.secret);
    webhook.name = data.name;
    webhook.url = data.url;
    return this.response(undefined);
  };

  handleDeleteWebhook = ({ webhook }: { webhook: string }) => {
    const index = this.webhooks.findIndex((w) => w.key === webhook);
    if (index === -1) {
      return Promise.reject(new Error('Webhook not found'));
    }
    this.webhooks.splice(index, 1);
    return this.response(undefined);
  };

  handleSearchDeliveries = ({
    webhook,
    ps = WebhooksMock.DEFAULT_PAGE_SIZE,
    p = 1,
  }: WebhookSearchDeliveriesPayload) => {
    const deliveries = mockFullWebhookDeliveries(webhook);
    const start = (p - 1) * ps;
    const end = start + ps;
    const paging = mockPaging({ pageIndex: p, pageSize: ps, total: deliveries.length });
    const page = deliveries.slice(start, end);
    return this.response({
      deliveries: page,
      paging,
    });
  };

  handleGetDelivery = ({ deliveryId }: { deliveryId: string }) => {
    const delivery = deliveries.find((delivery) => delivery.id === deliveryId);
    if (!delivery) {
      return Promise.reject(new Error('Delivery not found'));
    }
    return this.response({
      delivery: {
        ...delivery,
        payload: JSON.stringify({ id: delivery.id }),
      },
    });
  };
}
