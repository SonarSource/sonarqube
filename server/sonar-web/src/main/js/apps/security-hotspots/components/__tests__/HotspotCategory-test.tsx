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
import HotspotCategory, { HotspotCategoryProps } from '../HotspotCategory';

it('should render correctly', () => {
  expect(shallowRender().type()).toBeNull();
});

it('should render correctly with hotspots', () => {
  const securityCategory = 'command-injection';
  const hotspots = [
    mockRawHotspot({ key: 'h1', securityCategory }),
    mockRawHotspot({ key: 'h2', securityCategory }),
  ];
  expect(shallowRender({ hotspots })).toMatchSnapshot();
  expect(shallowRender({ hotspots, expanded: false })).toMatchSnapshot('collapsed');
  expect(
    shallowRender({ categoryKey: securityCategory, hotspots, selectedHotspot: hotspots[0] })
  ).toMatchSnapshot('contains selected');
  expect(shallowRender({ hotspots, isLastAndIncomplete: true })).toMatchSnapshot(
    'lastAndIncomplete'
  );
});

it('should handle collapse and expand', () => {
  const onToggleExpand = jest.fn();

  const categoryKey = 'xss-injection';

  const wrapper = shallowRender({
    categoryKey,
    expanded: true,
    hotspots: [mockRawHotspot()],
    onToggleExpand,
  });

  wrapper.find('.hotspot-category-header').simulate('click');

  expect(onToggleExpand).toHaveBeenCalledWith(categoryKey, false);

  wrapper.setProps({ expanded: false });
  wrapper.find('.hotspot-category-header').simulate('click');

  expect(onToggleExpand).toHaveBeenCalledWith(categoryKey, true);
});

function shallowRender(props: Partial<HotspotCategoryProps> = {}) {
  return shallow(
    <HotspotCategory
      categoryKey="xss-injection"
      expanded={true}
      hotspots={[]}
      onHotspotClick={jest.fn()}
      onToggleExpand={jest.fn()}
      selectedHotspot={mockRawHotspot()}
      title="Class Injection"
      isLastAndIncomplete={false}
      onLocationClick={jest.fn()}
      {...props}
    />
  );
}
