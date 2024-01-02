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
import React from 'react';
import { mockHotspot } from '../../../../helpers/mocks/security-hotspots';
import { HotspotStatusOption } from '../../../../types/security-hotspots';
import { HotspotHeader, HotspotHeaderProps } from '../HotspotHeader';
import Status from '../status/Status';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('correctly propagates the status change', () => {
  const onUpdateHotspot = jest.fn();
  const wrapper = shallowRender({ onUpdateHotspot });

  wrapper.find(Status).props().onStatusChange(HotspotStatusOption.FIXED);

  expect(onUpdateHotspot).toHaveBeenCalledWith(true, HotspotStatusOption.FIXED);
});

function shallowRender(props: Partial<HotspotHeaderProps> = {}) {
  return shallow(<HotspotHeader hotspot={mockHotspot()} onUpdateHotspot={jest.fn()} {...props} />);
}
