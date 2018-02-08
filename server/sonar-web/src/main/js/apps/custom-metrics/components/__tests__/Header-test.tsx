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
import Header from '../Header';

it('should create new metric', () => {
  const onCreate = jest.fn(() => Promise.resolve());
  const wrapper = shallow(
    <Header
      domains={['Coverage', 'Issues']}
      loading={false}
      onCreate={onCreate}
      types={['INT', 'STRING']}
    />
  );
  expect(wrapper).toMatchSnapshot();

  wrapper.find('CreateButton').prop<Function>('onCreate')({ key: 'foo', name: 'Foo', type: 'INT' });
  expect(onCreate).toBeCalledWith({ key: 'foo', name: 'Foo', type: 'INT' });
});
