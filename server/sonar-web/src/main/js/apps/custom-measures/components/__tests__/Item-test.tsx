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
import Item from '../Item';

const measure = {
  createdAt: '2017-01-01',
  description: 'my custom measure',
  id: '1',
  metric: { key: 'custom', name: 'custom-metric', type: 'STRING' },
  projectKey: 'foo',
  user: { active: true, login: 'user', name: 'user' },
  value: 'custom-value'
};

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should edit metric', () => {
  const onEdit = jest.fn();
  const wrapper = shallowRender({ onEdit });

  click(wrapper.find('.js-custom-measure-update'));
  wrapper.update();

  wrapper.find('Form').prop<Function>('onSubmit')({
    ...measure,
    description: 'new-description',
    value: 'new-value'
  });
  expect(onEdit).toBeCalledWith({ ...measure, description: 'new-description', value: 'new-value' });
});

it('should delete custom measure', () => {
  const onDelete = jest.fn();
  const wrapper = shallowRender({ onDelete });

  click(wrapper.find('.js-custom-measure-delete'));
  wrapper.update();

  wrapper.find('DeleteForm').prop<Function>('onSubmit')();
  expect(onDelete).toBeCalledWith('1');
});

it('should render correctly for deleted user', () => {
  expect(
    shallowRender({ measure: { ...measure, user: { active: false, login: 'user' } } })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<Item['props']> = {}) {
  return shallow(<Item measure={measure} onDelete={jest.fn()} onEdit={jest.fn()} {...props} />);
}
