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
import { deletePortfolio, deleteProject } from '../../../api/components';
import { mockRouter } from '../../../helpers/testMocks';
import { Form } from '../Form';

jest.mock('../../../api/components', () => ({
  deleteProject: jest.fn().mockResolvedValue(undefined),
  deletePortfolio: jest.fn().mockResolvedValue(undefined)
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render', () => {
  const component = { key: 'foo', name: 'Foo', qualifier: 'TRK' };
  const form = shallow(<Form component={component} router={mockRouter()} />);
  expect(form).toMatchSnapshot();
  expect(form.prop<Function>('children')({ onClick: jest.fn() })).toMatchSnapshot();
});

it('should delete project', async () => {
  const component = { key: 'foo', name: 'Foo', qualifier: 'TRK' };
  const router = mockRouter();
  const form = shallow(<Form component={component} router={router} />);
  form.prop<Function>('onConfirm')();
  expect(deleteProject).toBeCalledWith('foo');
  await new Promise(setImmediate);
  expect(router.replace).toBeCalledWith('/');
});

it('should delete portfolio', async () => {
  const component = { key: 'foo', name: 'Foo', qualifier: 'VW' };
  const router = mockRouter();
  const form = shallow(<Form component={component} router={router} />);
  form.prop<Function>('onConfirm')();
  expect(deletePortfolio).toBeCalledWith('foo');
  expect(deleteProject).not.toBeCalled();
  await new Promise(setImmediate);
  expect(router.replace).toBeCalledWith('/portfolios');
});
