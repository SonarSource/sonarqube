/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { deleteApplication } from '../../../api/application';
import { deletePortfolio, deleteProject } from '../../../api/components';
import { mockRouter } from '../../../helpers/testMocks';
import { Form } from '../Form';

jest.mock('../../../api/components', () => ({
  deleteProject: jest.fn().mockResolvedValue(undefined),
  deletePortfolio: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('../../../api/application', () => ({
  deleteApplication: jest.fn().mockResolvedValue(undefined),
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
  expect(deleteProject).toHaveBeenCalledWith('foo');
  await new Promise(setImmediate);
  expect(router.replace).toHaveBeenCalledWith('/');
});

it('should delete portfolio', async () => {
  const component = { key: 'foo', name: 'Foo', qualifier: 'VW' };
  const router = mockRouter();
  const form = shallow(<Form component={component} router={router} />);
  form.prop<Function>('onConfirm')();
  expect(deletePortfolio).toHaveBeenCalledWith('foo');
  expect(deleteProject).not.toHaveBeenCalled();
  expect(deleteApplication).not.toHaveBeenCalled();
  await new Promise(setImmediate);
  expect(router.replace).toHaveBeenCalledWith('/portfolios');
});

it('should delete application', async () => {
  const component = { key: 'foo', name: 'Foo', qualifier: 'APP' };
  const router = mockRouter();
  const form = shallow(<Form component={component} router={router} />);
  form.prop<Function>('onConfirm')();
  expect(deleteApplication).toHaveBeenCalledWith('foo');
  expect(deleteProject).not.toHaveBeenCalled();
  expect(deletePortfolio).not.toHaveBeenCalled();
  await new Promise(setImmediate);
  expect(router.replace).toHaveBeenCalledWith('/');
});
