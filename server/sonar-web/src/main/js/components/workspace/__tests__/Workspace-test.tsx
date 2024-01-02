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
import { mockBranch } from '../../../helpers/mocks/branch-like';
import { get, save } from '../../../helpers/storage';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { ComponentQualifier } from '../../../types/component';
import Workspace, { INITIAL_HEIGHT, MIN_HEIGHT, TYPE_KEY, WorkspaceTypes } from '../Workspace';

jest.mock('../../../helpers/storage', () => {
  return {
    get: jest.fn(),
    save: jest.fn(),
  };
});

jest.mock('../../../api/rules', () => ({
  getRulesApp: jest.fn().mockResolvedValue({
    repositories: [
      { key: 'foo', name: 'Foo' },
      { key: 'external_bar', name: 'Bar' },
    ],
  }),
}));

const WINDOW_HEIGHT = 1000;
const originalHeight = window.innerHeight;

beforeAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: WINDOW_HEIGHT,
  });
});

afterAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: originalHeight,
  });
});

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      components: [{ branchLike: mockBranch(), key: 'foo' }],
      open: { component: 'foo' },
    })
  ).toMatchSnapshot('open component');
});

it('should correctly load data from local storage', () => {
  const rule1 = { [TYPE_KEY]: WorkspaceTypes.Rule, key: 'foo' };
  const rule2 = {
    [TYPE_KEY]: WorkspaceTypes.Rule,
    key: 'baz',
    name: 'Baz',
  };
  const component = {
    [TYPE_KEY]: WorkspaceTypes.Component,
    branchLike: mockBranch(),
    key: 'foo',
  };
  (get as jest.Mock).mockReturnValue(JSON.stringify([rule1, rule2, component]));

  let wrapper = shallowRender();
  expect(wrapper.state().rules).toEqual([rule1, rule2]);
  expect(wrapper.state().components).toEqual([component]);

  (get as jest.Mock).mockImplementationOnce(() => {
    throw Error('No local storage');
  });
  wrapper = shallowRender();
  expect(wrapper.state().rules).toEqual([]);
  expect(wrapper.state().components).toEqual([]);
});

it('should correctly store data locally', () => {
  const wrapper = shallowRender({
    components: [{ branchLike: mockBranch(), key: 'foo' }],
    rules: [{ key: 'foo' }],
  });
  wrapper.instance().saveWorkspace();
  expect((save as jest.Mock).mock.calls[0][1]).toMatchSnapshot();
});

it('should load rule engine names', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().externalRulesRepoNames).toEqual({ bar: 'Bar' });
});

it('should allow elements to be loaded and updated', () => {
  const component = { key: 'foo', branchLike: mockBranch() };
  const rule = { key: 'bar' };
  const wrapper = shallowRender({
    components: [component],
    rules: [rule],
  });
  const instance = wrapper.instance();

  instance.handleComponentLoad({ key: 'baz', name: 'Baz', qualifier: ComponentQualifier.TestFile });
  expect(wrapper.state().components).toEqual([component]);

  instance.handleComponentLoad({ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.File });
  expect(wrapper.state().components).toEqual([
    { ...component, name: 'Foo', qualifier: ComponentQualifier.File },
  ]);
});

it('should be resizable', () => {
  (get as jest.Mock).mockReturnValue(
    JSON.stringify([{ [TYPE_KEY]: WorkspaceTypes.Rule, key: 'foo' }])
  );
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  instance.handleMaximize();
  expect(wrapper.state().maximized).toBe(true);

  instance.handleMinimize();
  expect(wrapper.state().maximized).toBe(false);

  instance.handleResize(-200);
  expect(wrapper.state().height).toBe(INITIAL_HEIGHT + 200);

  instance.handleResize(1000);
  expect(wrapper.state().height).toBe(WINDOW_HEIGHT * MIN_HEIGHT);
});

it('should be openable/collapsible', () => {
  const component = {
    branchLike: mockBranch(),
    key: 'foo',
  };
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  instance.handleOpenComponent(component);
  expect(wrapper.state().open).toEqual({ component: 'foo' });

  instance.handleCollapse();
  expect(wrapper.state().open).toEqual({});

  instance.handleComponentReopen(component.key);
  expect(wrapper.state().open).toEqual({ component: 'foo' });

  instance.handleComponentClose('bar');
  expect(wrapper.state().open).toEqual({ component: 'foo' });
  instance.handleComponentClose('foo');
  expect(wrapper.state().open).toEqual({});
});

function shallowRender(state?: Partial<Workspace['state']>) {
  const wrapper = shallow<Workspace>(
    <Workspace>
      <div className="child" />
    </Workspace>
  );
  return state ? wrapper.setState(state as Workspace['state']) : wrapper;
}
