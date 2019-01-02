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
import { shallow } from 'enzyme';
import PageActions from '../PageActions';
import { click } from '../../../../helpers/testUtils';

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should not render', () => {
  expect(getWrapper({ loading: true }).type()).toBeNull();
});

it('should not allow to create a new webhook', () => {
  expect(getWrapper({ webhooksCount: 10 })).toMatchSnapshot();
});

it('should display the create form', () => {
  const onCreate = jest.fn();
  const wrapper = getWrapper({ onCreate });
  click(wrapper.find('.js-webhook-create'));
  expect(wrapper.find('CreateWebhookForm').exists()).toBeTruthy();
  wrapper.find('CreateWebhookForm').prop<Function>('onDone')({
    name: 'foo',
    url: 'http://foo.bar'
  });
  expect(onCreate).lastCalledWith({ name: 'foo', url: 'http://foo.bar' });
});

function getWrapper(props = {}) {
  return shallow(<PageActions loading={false} onCreate={jest.fn()} webhooksCount={5} {...props} />);
}
