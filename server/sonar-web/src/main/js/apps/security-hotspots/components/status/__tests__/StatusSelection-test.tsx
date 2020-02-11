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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { setSecurityHotspotStatus } from '../../../../../api/security-hotspots';
import { mockHotspot } from '../../../../../helpers/mocks/security-hotspots';
import { HotspotStatus, HotspotStatusOption } from '../../../../../types/security-hotspots';
import StatusSelection from '../StatusSelection';
import StatusSelectionRenderer from '../StatusSelectionRenderer';

jest.mock('../../../../../api/security-hotspots', () => ({
  setSecurityHotspotStatus: jest.fn()
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should properly deal with comment/status/submit events', async () => {
  const hotspot = mockHotspot();
  const onStatusOptionChange = jest.fn();
  const wrapper = shallowRender({ hotspot, onStatusOptionChange });

  const newStatusOption = HotspotStatusOption.SAFE;
  wrapper
    .find(StatusSelectionRenderer)
    .props()
    .onStatusChange(newStatusOption);
  expect(wrapper.state().selectedStatus).toBe(newStatusOption);
  expect(wrapper.find(StatusSelectionRenderer).props().submitDisabled).toBe(false);

  const newComment = 'TEST-COMMENT';
  wrapper
    .find(StatusSelectionRenderer)
    .props()
    .onCommentChange(newComment);
  expect(wrapper.state().comment).toBe(newComment);

  (setSecurityHotspotStatus as jest.Mock).mockResolvedValueOnce({});
  wrapper
    .find(StatusSelectionRenderer)
    .props()
    .onSubmit();
  expect(setSecurityHotspotStatus).toHaveBeenCalledWith(hotspot.key, {
    status: HotspotStatus.REVIEWED,
    resolution: HotspotStatusOption.SAFE,
    comment: newComment
  });

  await waitAndUpdate(wrapper);

  expect(onStatusOptionChange).toHaveBeenCalledWith(newStatusOption);
});

function shallowRender(props?: Partial<StatusSelection['props']>) {
  return shallow<StatusSelection>(
    <StatusSelection hotspot={mockHotspot()} onStatusOptionChange={jest.fn()} {...props} />
  );
}
