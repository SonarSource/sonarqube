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
import { mockRawHotspot } from '../../../../helpers/mocks/security-hotspots';
import HotspotCategory, { HotspotCategoryProps } from '../HotspotCategory';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('empty');
});

it('should render correctly with hotspots', () => {
  const hotspots = [mockRawHotspot({ key: 'h1' }), mockRawHotspot({ key: 'h2' })];
  expect(shallowRender({ hotspots })).toMatchSnapshot();
  expect(shallowRender({ hotspots, startsExpanded: false })).toMatchSnapshot('collapsed');
});

it('should handle collapse and expand', () => {
  const wrapper = shallowRender({ hotspots: [mockRawHotspot()] });

  wrapper.find('.hotspot-category-header').simulate('click');

  expect(wrapper).toMatchSnapshot();

  wrapper.find('.hotspot-category-header').simulate('click');

  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<HotspotCategoryProps> = {}) {
  return shallow(
    <HotspotCategory
      hotspots={[]}
      onHotspotClick={jest.fn()}
      selectedHotspotKey=""
      startsExpanded={true}
      title="Class Injection"
      {...props}
    />
  );
}
