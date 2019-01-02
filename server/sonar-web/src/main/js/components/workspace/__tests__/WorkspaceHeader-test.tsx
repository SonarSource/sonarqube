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
import WorkspaceHeader, { Props } from '../WorkspaceHeader';

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should resize', () => {
  const onResize = jest.fn();
  const wrapper = shallowRender({ onResize });
  wrapper.find('DraggableCore').prop<Function>('onDrag')({}, { deltaY: 15 });
  expect(onResize).toBeCalledWith(15);
});

function shallowRender(props?: Partial<Props>) {
  return shallow(
    <WorkspaceHeader
      onClose={jest.fn()}
      onCollapse={jest.fn()}
      onMaximize={jest.fn()}
      onMinimize={jest.fn()}
      onResize={jest.fn()}
      {...props}>
      <div id="workspace-header-children" />
    </WorkspaceHeader>
  );
}
