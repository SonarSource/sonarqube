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
import { EditButton } from '../../../../../components/controls/buttons';
import OutsideClickHandler from '../../../../../components/controls/OutsideClickHandler';
import { mockLoggedInUser, mockUser } from '../../../../../helpers/testMocks';
import { click } from '../../../../../helpers/testUtils';
import { UserActive } from '../../../../../types/users';
import AssigneeRenderer, { AssigneeRendererProps } from '../AssigneeRenderer';
import AssigneeSelection from '../AssigneeSelection';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('not editing');
  expect(shallowRender({ editing: true })).toMatchSnapshot('editing');
  expect(shallowRender({ assignee: undefined })).toMatchSnapshot('without current assignee');
  expect(shallowRender({ assignee: mockUser({ active: true }) })).toMatchSnapshot(
    'with active assignee'
  );

  expect(shallowRender({ loggedInUser: undefined }).find(EditButton).length).toBe(0);
  expect(shallowRender({ canEdit: false }).find(EditButton).length).toBe(0);
  expect(
    shallowRender({ editing: true, assignee: mockUser() }).find(AssigneeSelection).props()
      .allowCurrentUserSelection
  ).toBe(true);
});

it('should propagate calls correctly', () => {
  const onAssign = jest.fn();
  const onEnterEditionMode = jest.fn();
  const onExitEditionMode = jest.fn();
  const wrapper = shallowRender({ onAssign, onEnterEditionMode, onExitEditionMode });

  click(wrapper.find(EditButton));
  expect(onEnterEditionMode).toHaveBeenCalled();

  const newAssignee = mockUser({ login: 'toto' });
  wrapper.setProps({ editing: true });
  wrapper
    .find(AssigneeSelection)
    .props()
    .onSelect(newAssignee as UserActive);
  expect(onAssign).toHaveBeenCalledWith(newAssignee);

  wrapper.find(OutsideClickHandler).props().onClickOutside();
  expect(onExitEditionMode).toHaveBeenCalled();
});

function shallowRender(props?: Partial<AssigneeRendererProps>) {
  return shallow<AssigneeRendererProps>(
    <AssigneeRenderer
      assignee={mockLoggedInUser()}
      canEdit={true}
      editing={false}
      loading={false}
      loggedInUser={mockLoggedInUser()}
      onAssign={jest.fn()}
      onEnterEditionMode={jest.fn()}
      onExitEditionMode={jest.fn()}
      {...props}
    />
  );
}
