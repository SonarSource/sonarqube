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
import { shallow } from 'enzyme';
import WebhookActions from '../WebhookActions';
import { click } from '../../../../helpers/testUtils';

const webhook = { key: '1', name: 'foo', url: 'http://foo.bar' };

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
  click(
    wrapper
      .find('ConfirmButton')
      .dive()
      .find('.js-webhook-delete')
  );
  expect(wrapper.find('ConfirmButton').exists()).toBeTruthy();
  wrapper.find('ConfirmButton').prop<Function>('onConfirm')();
  expect(onDelete).lastCalledWith(webhook.key);
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
