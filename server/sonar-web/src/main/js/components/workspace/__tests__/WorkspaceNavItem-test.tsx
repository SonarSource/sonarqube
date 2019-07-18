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
import { click } from 'sonar-ui-common/helpers/testUtils';
import WorkspaceNavItem, { Props } from '../WorkspaceNavItem';

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should close', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose });
  click(wrapper.find('ClearButton'));
  expect(onClose).toBeCalled();
});

it('should open', () => {
  const onOpen = jest.fn();
  const wrapper = shallowRender({ onOpen });
  click(wrapper.find('.workspace-nav-item-link'));
  expect(onOpen).toBeCalled();
});

function shallowRender(props?: Partial<Props>) {
  return shallow(
    <WorkspaceNavItem onClose={jest.fn()} onOpen={jest.fn()} {...props}>
      <div id="workspace-nav-item" />
    </WorkspaceNavItem>
  );
}
