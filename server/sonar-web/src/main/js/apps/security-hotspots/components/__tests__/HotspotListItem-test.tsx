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
import { mockRawHotspot } from '../../../../helpers/mocks/security-hotspots';
import HotspotListItem, { HotspotListItemProps } from '../HotspotListItem';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ selected: true })).toMatchSnapshot();
});

it('should handle click', () => {
  const hotspot = mockRawHotspot({ key: 'hotspotKey' });
  const onClick = jest.fn();
  const wrapper = shallowRender({ hotspot, onClick });

  wrapper.simulate('click');

  expect(onClick).toHaveBeenCalledWith(hotspot);
});

it('should handle click on the title', () => {
  const hotspot = mockRawHotspot({ key: 'hotspotKey' });
  const onLocationClick = jest.fn();
  const wrapper = shallowRender({ hotspot, onLocationClick, selected: true });

  wrapper.find('div.cursor-pointer').simulate('click');

  expect(onLocationClick).toHaveBeenCalledWith();
});

function shallowRender(props: Partial<HotspotListItemProps> = {}) {
  return shallow(
    <HotspotListItem
      hotspot={mockRawHotspot()}
      onClick={jest.fn()}
      onLocationClick={jest.fn}
      selected={false}
      {...props}
    />
  );
}
