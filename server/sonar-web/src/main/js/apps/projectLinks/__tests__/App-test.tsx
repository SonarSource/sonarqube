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
import { createLink, deleteLink, getProjectLinks } from '../../../api/projectLinks';
import { mockComponent } from '../../../helpers/mocks/component';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { App } from '../App';

// import { getProjectLinks, createLink, deleteLink } from '../../api/projectLinks';
jest.mock('../../../api/projectLinks', () => ({
  getProjectLinks: jest.fn().mockResolvedValue([
    { id: '1', type: 'homepage', url: 'http://example.com' },
    { id: '2', name: 'foo', type: 'foo', url: 'http://example.com/foo' },
  ]),
  createLink: jest
    .fn()
    .mockResolvedValue({ id: '3', name: 'bar', type: 'bar', url: 'http://example.com/bar' }),
  deleteLink: jest.fn().mockResolvedValue(undefined),
}));

it('should fetch links and render', async () => {
  const wrapper = shallow(<App component={mockComponent({ key: 'comp' })} />);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(getProjectLinks).toHaveBeenCalledWith('comp');
});

it('should fetch links when component changes', async () => {
  const wrapper = shallow(<App component={mockComponent({ key: 'comp' })} />);
  await waitAndUpdate(wrapper);
  expect(getProjectLinks).toHaveBeenLastCalledWith('comp');

  wrapper.setProps({ component: { key: 'another' } });
  expect(getProjectLinks).toHaveBeenLastCalledWith('another');
});

it('should create link', async () => {
  const wrapper = shallow(<App component={mockComponent({ key: 'comp' })} />);
  await waitAndUpdate(wrapper);

  wrapper.find('Header').prop<Function>('onCreate')('bar', 'http://example.com/bar');
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(createLink).toHaveBeenCalledWith({
    name: 'bar',
    projectKey: 'comp',
    url: 'http://example.com/bar',
  });
});

it('should delete link', async () => {
  const wrapper = shallow(<App component={mockComponent({ key: 'comp' })} />);
  await waitAndUpdate(wrapper);

  wrapper.find('Table').prop<Function>('onDelete')('foo');
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(deleteLink).toHaveBeenCalledWith('foo');
});
