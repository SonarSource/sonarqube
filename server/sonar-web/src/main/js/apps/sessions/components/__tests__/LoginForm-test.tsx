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
import LoginForm from '../LoginForm';
import { change, click, submit, waitAndUpdate } from '../../../../helpers/testUtils';

it('logs in with simple credentials', () => {
  const onSubmit = jest.fn(() => Promise.resolve());
  const wrapper = shallow(<LoginForm onSubmit={onSubmit} returnTo="" />);
  expect(wrapper).toMatchSnapshot();

  change(wrapper.find('#login'), 'admin');
  change(wrapper.find('#password'), 'admin');
  submit(wrapper.find('form'));

  expect(onSubmit).toBeCalledWith('admin', 'admin');
});

it('should display a spinner and disabled button while loading', async () => {
  const onSubmit = jest.fn(() => Promise.resolve());
  const wrapper = shallow(<LoginForm onSubmit={onSubmit} returnTo="" />);

  change(wrapper.find('#login'), 'admin');
  change(wrapper.find('#password'), 'admin');
  submit(wrapper.find('form'));
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
});

it('expands more options', () => {
  const wrapper = shallow(<LoginForm collapsed={true} onSubmit={jest.fn()} returnTo="" />);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.js-more-options'));
  expect(wrapper).toMatchSnapshot();
});
