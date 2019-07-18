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

const metric = { id: '3', key: 'foo', name: 'Foo', type: 'INT' };

it('should render', () => {
  expect(
    shallow(<Item metric={metric} onDelete={jest.fn()} onEdit={jest.fn()} />)
  ).toMatchSnapshot();
});

it('should edit metric', () => {
  const onEdit = jest.fn();

  const wrapper = shallow(
    <Item
      domains={['Coverage', 'Issues']}
      metric={metric}
      onDelete={jest.fn()}
      onEdit={onEdit}
      types={['INT', 'STRING']}
    />
  );

  click(wrapper.find('.js-metric-update'));
  wrapper.update();

  wrapper.find('Form').prop<Function>('onSubmit')({
    ...metric,
    description: 'bla bla',
    domain: 'Coverage'
  });
  expect(onEdit).toBeCalledWith({ ...metric, description: 'bla bla', domain: 'Coverage' });
});

it('should delete metric', () => {
  const onDelete = jest.fn();
  const wrapper = shallow(<Item metric={metric} onDelete={onDelete} onEdit={jest.fn()} />);

  click(wrapper.find('.js-metric-delete'));
  wrapper.update();

  wrapper.find('DeleteForm').prop<Function>('onSubmit')();
  expect(onDelete).toBeCalledWith('foo');
});
