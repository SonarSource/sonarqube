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
import WorkspaceNavComponent, { Props } from '../WorkspaceNavComponent';

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should close', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose });
  wrapper.find('WorkspaceNavItem').prop<Function>('onClose')();
  expect(onClose).toBeCalledWith('foo');
});

it('should open', () => {
  const onOpen = jest.fn();
  const wrapper = shallowRender({ onOpen });
  wrapper.find('WorkspaceNavItem').prop<Function>('onOpen')();
  expect(onOpen).toBeCalledWith('foo');
});

function shallowRender(props?: Partial<Props>) {
  const component = { branchLike: undefined, key: 'foo' };
  return shallow(
    <WorkspaceNavComponent
      component={component}
      onClose={jest.fn()}
      onOpen={jest.fn()}
      {...props}
    />
  );
}
