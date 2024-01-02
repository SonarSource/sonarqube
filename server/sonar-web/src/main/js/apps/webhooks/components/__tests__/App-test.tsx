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
import {
  createWebhook,
  deleteWebhook,
  searchWebhooks,
  updateWebhook,
} from '../../../../api/webhooks';
import { mockComponent } from '../../../../helpers/mocks/component';
import { App } from '../App';

jest.mock('../../../../api/webhooks', () => ({
  createWebhook: jest.fn(() =>
    Promise.resolve({ webhook: { key: '3', name: 'baz', url: 'http://baz', hasSecret: false } })
  ),
  deleteWebhook: jest.fn(() => Promise.resolve()),
  searchWebhooks: jest.fn(() =>
    Promise.resolve({
      webhooks: [
        { key: '1', name: 'foo', url: 'http://foo', hasSecret: false },
        { key: '2', name: 'bar', url: 'http://bar', hasSecret: false },
      ],
    })
  ),
  updateWebhook: jest.fn(() => Promise.resolve()),
}));

const component = mockComponent({ key: 'bar', qualifier: 'TRK' });

beforeEach(() => {
  (createWebhook as jest.Mock<any>).mockClear();
  (deleteWebhook as jest.Mock<any>).mockClear();
  (searchWebhooks as jest.Mock<any>).mockClear();
  (updateWebhook as jest.Mock<any>).mockClear();
});

it('should be in loading status', () => {
  expect(shallow(<App />)).toMatchSnapshot();
});

it('should fetch webhooks and display them', async () => {
  const wrapper = shallow(<App />);
  expect(wrapper.state('loading')).toBe(true);

  await new Promise(setImmediate);
  expect(searchWebhooks).toHaveBeenCalledWith({});

  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

describe('should correctly fetch webhooks when', () => {
  it('on global scope', async () => {
    shallow(<App />);

    await new Promise(setImmediate);
    expect(searchWebhooks).toHaveBeenCalledWith({ projects: undefined });
  });

  it('on project scope', async () => {
    shallow(<App component={component} />);

    await new Promise(setImmediate);
    expect(searchWebhooks).toHaveBeenCalledWith({
      project: component.key,
    });
  });
});

it('should correctly handle webhook creation', async () => {
  const webhook = { name: 'baz', url: 'http://baz' };
  const wrapper = shallow(<App />);
  (wrapper.instance() as App).handleCreate({ ...webhook });
  expect(createWebhook).toHaveBeenLastCalledWith({
    ...webhook,
    project: undefined,
  });

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.state('webhooks')).toEqual([
    { key: '1', name: 'foo', url: 'http://foo', hasSecret: false },
    { key: '2', name: 'bar', url: 'http://bar', hasSecret: false },
    { key: '3', name: 'baz', url: 'http://baz', hasSecret: false },
  ]);
});

it('should correctly handle webhook deletion', async () => {
  const wrapper = shallow(<App />);
  (wrapper.instance() as App).handleDelete('2');
  expect(deleteWebhook).toHaveBeenLastCalledWith({ webhook: '2' });

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.state('webhooks')).toEqual([
    { key: '1', name: 'foo', url: 'http://foo', hasSecret: false },
  ]);
});

it('should correctly handle webhook update', async () => {
  const newValues = { webhook: '1', name: 'Cfoo', url: 'http://cfoo', secret: undefined };
  const wrapper = shallow(<App />);
  (wrapper.instance() as App).handleUpdate(newValues);
  expect(updateWebhook).toHaveBeenLastCalledWith(newValues);

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.state('webhooks')).toEqual([
    { key: '1', name: 'Cfoo', url: 'http://cfoo', hasSecret: false },
    { key: '2', name: 'bar', url: 'http://bar', hasSecret: false },
  ]);
});

it('should correctly handle webhook secret update', async () => {
  const newValuesWithSecret = { webhook: '2', name: 'bar', url: 'http://bar', secret: 'secret' };
  const newValuesWithoutSecret = {
    webhook: '2',
    name: 'bar',
    url: 'http://bar',
    secret: undefined,
  };
  const newValuesWithEmptySecret = { webhook: '2', name: 'bar', url: 'http://bar', secret: '' };
  const wrapper = shallow(<App />);

  // With secret
  (wrapper.instance() as App).handleUpdate(newValuesWithSecret);
  expect(updateWebhook).toHaveBeenLastCalledWith(newValuesWithSecret);

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.state('webhooks')).toEqual([
    { key: '1', name: 'foo', url: 'http://foo', hasSecret: false },
    { key: '2', name: 'bar', url: 'http://bar', hasSecret: true },
  ]);

  // Without secret
  (wrapper.instance() as App).handleUpdate(newValuesWithoutSecret);
  expect(updateWebhook).toHaveBeenLastCalledWith(newValuesWithoutSecret);

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.state('webhooks')).toEqual([
    { key: '1', name: 'foo', url: 'http://foo', hasSecret: false },
    { key: '2', name: 'bar', url: 'http://bar', hasSecret: true },
  ]);

  // With empty secret
  (wrapper.instance() as App).handleUpdate(newValuesWithEmptySecret);
  expect(updateWebhook).toHaveBeenLastCalledWith(newValuesWithEmptySecret);

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.state('webhooks')).toEqual([
    { key: '1', name: 'foo', url: 'http://foo', hasSecret: false },
    { key: '2', name: 'bar', url: 'http://bar', hasSecret: false },
  ]);
});
