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
import { WithRouterProps } from 'react-router';
import { Key } from '../Key';
import { changeKey } from '../../../api/components';

jest.mock('../../../api/components', () => ({
  changeKey: jest.fn().mockResolvedValue(undefined)
}));

it('should render and change key', async () => {
  const withRouterProps = { router: { replace: jest.fn() } as any } as WithRouterProps;
  const wrapper = shallow(<Key component={{ key: 'foo', name: 'Foo' }} {...withRouterProps} />);
  expect(wrapper).toMatchSnapshot();

  wrapper.find('UpdateForm').prop<Function>('onKeyChange')('bar');
  await new Promise(setImmediate);
  expect(changeKey).toBeCalledWith({ from: 'foo', to: 'bar' });
  expect(withRouterProps.router.replace).toBeCalledWith({
    pathname: '/project/key',
    query: { id: 'bar' }
  });
});
