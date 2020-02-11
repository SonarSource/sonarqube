/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import { click } from 'sonar-ui-common/helpers/testUtils';
import { mockHotspot } from '../../../../../helpers/mocks/security-hotspots';
import { mockCurrentUser } from '../../../../../helpers/testMocks';
import { HotspotStatusOption } from '../../../../../types/security-hotspots';
import { Status, StatusProps } from '../Status';
import StatusSelection from '../StatusSelection';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('closed');

  click(wrapper.find('#status-trigger'));
  expect(wrapper).toMatchSnapshot('open');

  wrapper
    .find(Toggler)
    .props()
    .onRequestClose();
  expect(wrapper.find(DropdownOverlay).length).toBe(0);

  expect(shallowRender({ hotspot: mockHotspot({ canChangeStatus: false }) })).toMatchSnapshot(
    'readonly'
  );
});

it('should properly deal with status changes', () => {
  const onStatusChange = jest.fn();
  const wrapper = shallowRender({ onStatusChange });

  click(wrapper.find('#status-trigger'));
  wrapper
    .find(Toggler)
    .dive()
    .find(StatusSelection)
    .props()
    .onStatusOptionChange(HotspotStatusOption.SAFE);
  expect(onStatusChange).toHaveBeenCalled();
  expect(wrapper.find(DropdownOverlay).length).toBe(0);
});

function shallowRender(props?: Partial<StatusProps>) {
  return shallow<StatusProps>(
    <Status
      currentUser={mockCurrentUser({ isLoggedIn: true })}
      hotspot={mockHotspot()}
      onStatusChange={jest.fn()}
      {...props}
    />
  );
}
