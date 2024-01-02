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
import { assignSecurityHotspot } from '../../../../../api/security-hotspots';
import { addGlobalSuccessMessage } from '../../../../../helpers/globalMessages';
import { mockHotspot } from '../../../../../helpers/mocks/security-hotspots';
import { mockCurrentUser, mockUser } from '../../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../../helpers/testUtils';
import { HotspotResolution, HotspotStatus } from '../../../../../types/security-hotspots';
import { UserActive } from '../../../../../types/users';
import { Assignee } from '../Assignee';
import AssigneeRenderer from '../AssigneeRenderer';

jest.mock('../../../../../api/security-hotspots', () => ({
  assignSecurityHotspot: jest.fn(),
}));

jest.mock('../../../../../helpers/globalMessages', () => ({
  addGlobalSuccessMessage: jest.fn(),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it.each([
  [HotspotStatus.TO_REVIEW, undefined, true],
  [HotspotStatus.REVIEWED, HotspotResolution.FIXED, false],
  [HotspotStatus.REVIEWED, HotspotResolution.SAFE, false],
  [HotspotStatus.REVIEWED, HotspotResolution.ACKNOWLEDGED, true],
])('should allow edition properly', (status, resolution, canEdit) => {
  expect(
    shallowRender({ hotspot: mockHotspot({ status, resolution }) })
      .find(AssigneeRenderer)
      .props().canEdit
  ).toBe(canEdit);
});

it('should handle edition event correctly', () => {
  const wrapper = shallowRender();

  wrapper.find(AssigneeRenderer).props().onEnterEditionMode();
  expect(wrapper.state().editing).toBe(true);

  wrapper.find(AssigneeRenderer).props().onExitEditionMode();
  expect(wrapper.state().editing).toBe(false);
});

it.each([
  ['assign to user', mockUser() as UserActive],
  ['unassign', { login: '', name: 'unassigned' } as UserActive],
])('should handle %s event', async (_, user: UserActive) => {
  const hotspot = mockHotspot();
  const onAssigneeChange = jest.fn();

  const wrapper = shallowRender({ hotspot, onAssigneeChange });

  (assignSecurityHotspot as jest.Mock).mockResolvedValueOnce({});
  wrapper.find(AssigneeRenderer).props().onAssign(user);

  expect(wrapper.state().loading).toBe(true);
  expect(assignSecurityHotspot).toHaveBeenCalledWith(hotspot.key, { assignee: user?.login });

  await waitAndUpdate(wrapper);

  expect(wrapper.state()).toEqual({
    editing: false,
    loading: false,
  });
  expect(onAssigneeChange).toHaveBeenCalled();
  expect(addGlobalSuccessMessage).toHaveBeenCalled();
});

function shallowRender(props?: Partial<Assignee['props']>) {
  return shallow<Assignee>(
    <Assignee
      currentUser={mockCurrentUser()}
      hotspot={mockHotspot()}
      onAssigneeChange={jest.fn()}
      {...props}
    />
  );
}
