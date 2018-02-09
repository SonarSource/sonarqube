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
import Form from '../Form';
import { change, submit, click } from '../../../../helpers/testUtils';

it('should render form', async () => {
  const onClose = jest.fn();
  const onSubmit = jest.fn(() => Promise.resolve());
  const wrapper = shallow(
    <Form
      confirmButtonText="confirmButtonText"
      domains={['Coverage', 'Issues']}
      header="header"
      onClose={onClose}
      onSubmit={onSubmit}
      types={['INT', 'STRING']}
    />
  ).dive();
  expect(wrapper).toMatchSnapshot();

  change(wrapper.find('[name="key"]'), 'foo');
  change(wrapper.find('[name="name"]'), 'Foo');
  change(wrapper.find('[name="description"]'), 'bar');
  wrapper.find('Creatable').prop<Function>('onChange')({ value: 'Coverage' });
  submit(wrapper.find('form'));
  expect(onSubmit).toBeCalledWith({
    description: 'bar',
    domain: 'Coverage',
    key: 'foo',
    name: 'Foo',
    type: 'INT'
  });

  await new Promise(setImmediate);
  expect(onClose).toBeCalled();

  onClose.mockClear();
  click(wrapper.find('button[type="reset"]'));
  expect(onClose).toBeCalled();
});
