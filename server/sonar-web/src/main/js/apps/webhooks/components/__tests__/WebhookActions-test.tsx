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
import { shallow } from 'enzyme';
import * as React from 'react';
import { click } from 'sonar-ui-common/helpers/testUtils';
import WebhookActions from '../WebhookActions';

const webhook = {
  key: '1',
  name: 'foo',
  url: 'http://foo.bar'
};

const delivery = {
  at: '12.02.2018',
  durationMs: 20,
  httpStatus: 200,
  id: '2',
  success: true
};

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should display the update webhook form', () => {
  const onUpdate = jest.fn(() => Promise.resolve());
  const wrapper = getWrapper({ onUpdate });
  click(wrapper.find('.js-webhook-update'));
  expect(wrapper.find('CreateWebhookForm').exists()).toBeTruthy();
  wrapper.find('CreateWebhookForm').prop<Function>('onDone')({
    name: webhook.name,
    url: webhook.url
  });
  expect(onUpdate).lastCalledWith({ webhook: webhook.key, name: webhook.name, url: webhook.url });
});

it('should display the delete webhook form', () => {
  const onDelete = jest.fn(() => Promise.resolve());
  const wrapper = getWrapper({ onDelete });
  click(wrapper.find('.js-webhook-delete'));
  expect(wrapper.find('DeleteWebhookForm').exists()).toBeTruthy();
  wrapper.find('DeleteWebhookForm').prop<Function>('onSubmit')();
  expect(onDelete).lastCalledWith(webhook.key);
});

it('should display the deliveries form', () => {
  const wrapper = getWrapper({ webhook: { ...webhook, latestDelivery: delivery } });
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('.js-webhook-deliveries'));
  expect(wrapper.find('DeliveriesForm').exists()).toBeTruthy();
});

function getWrapper(props = {}) {
  return shallow(
    <WebhookActions
      onDelete={jest.fn(() => Promise.resolve())}
      onUpdate={jest.fn(() => Promise.resolve())}
      webhook={webhook}
      {...props}
    />
  );
}
