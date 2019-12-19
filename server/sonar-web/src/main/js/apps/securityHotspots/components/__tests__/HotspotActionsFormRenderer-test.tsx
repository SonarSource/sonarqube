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
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { HotspotStatusOptions } from '../../../../types/security-hotspots';
import HotspotActionsForm from '../HotspotActionsForm';
import HotspotActionsFormRenderer, {
  HotspotActionsFormRendererProps
} from '../HotspotActionsFormRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ submitting: true })).toMatchSnapshot('Submitting');
  expect(shallowRender({ selectedOption: HotspotStatusOptions.SAFE })).toMatchSnapshot(
    'safe option selected'
  );
  expect(
    shallowRender({
      selectedOption: HotspotStatusOptions.ADDITIONAL_REVIEW,
      selectedUser: mockLoggedInUser()
    })
  ).toMatchSnapshot('user selected');
});

function shallowRender(props: Partial<HotspotActionsFormRendererProps> = {}) {
  return shallow<HotspotActionsForm>(
    <HotspotActionsFormRenderer
      hotspotKey="key"
      onAssign={jest.fn()}
      onSelectOption={jest.fn()}
      onSubmit={jest.fn()}
      selectedOption={HotspotStatusOptions.FIXED}
      submitting={false}
      {...props}
    />
  );
}
