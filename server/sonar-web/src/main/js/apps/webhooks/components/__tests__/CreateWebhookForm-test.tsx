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
import CreateWebhookForm from '../CreateWebhookForm';

const webhookWithoutSecret = { key: '1', name: 'foo', url: 'http://foo.bar' };
const webhookWithSecret = { key: '2', name: 'bar', secret: 'sonar', url: 'http://foo.bar' };

it('should render correctly when creating a new webhook', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should render correctly when updating a webhook without secret', () => {
  expect(getWrapper({ webhook: webhookWithoutSecret })).toMatchSnapshot();
});

it('should render correctly when updating a webhook with a secret', () => {
  expect(getWrapper({ webhook: webhookWithSecret })).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <CreateWebhookForm onClose={jest.fn()} onDone={jest.fn(() => Promise.resolve())} {...props} />
  );
}
