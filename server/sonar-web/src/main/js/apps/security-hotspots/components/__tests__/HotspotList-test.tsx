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
import { addSideBarClass, removeSideBarClass } from '../../../../helpers/pages';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { HotspotStatusFilter, RiskExposure } from '../../../../types/security-hotspots';
import HotspotList from '../HotspotList';

jest.mock('../../../../helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  removeSideBarClass: jest.fn(),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ loadingMore: true })).toMatchSnapshot();
});

it('should add/remove sidebar classes', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(addSideBarClass).toHaveBeenCalled();

  wrapper.unmount();

  expect(removeSideBarClass).toHaveBeenCalled();
});

it('should render correctly when the list of hotspot is static', () => {
  expect(shallowRender({ isStaticListOfHotspots: true })).toMatchSnapshot();
});

const hotspots = [
  mockRawHotspot({ key: 'h1', securityCategory: 'cat2' }),
  mockRawHotspot({ key: 'h2', securityCategory: 'cat1' }),
  mockRawHotspot({
    key: 'h3',
    securityCategory: 'cat1',
    vulnerabilityProbability: RiskExposure.MEDIUM,
  }),
  mockRawHotspot({
    key: 'h4',
    securityCategory: 'cat1',
    vulnerabilityProbability: RiskExposure.MEDIUM,
  }),
  mockRawHotspot({
    key: 'h5',
    securityCategory: 'cat2',
    vulnerabilityProbability: RiskExposure.MEDIUM,
  }),
];

it('should render correctly with hotspots', () => {
  expect(shallowRender({ hotspots, hotspotsTotal: hotspots.length })).toMatchSnapshot(
    'no pagination'
  );
  expect(shallowRender({ hotspots, hotspotsTotal: 7 })).toMatchSnapshot('pagination');
});

it('should update expanded categories correctly', () => {
  const wrapper = shallowRender({ hotspots, selectedHotspot: hotspots[0] });

  expect(wrapper.state().expandedCategories).toEqual({ cat2: true });

  wrapper.setProps({ selectedHotspot: hotspots[1] });

  expect(wrapper.state().expandedCategories).toEqual({ cat1: true, cat2: true });
});

it('should update grouped hotspots when the list changes', () => {
  const wrapper = shallowRender({ hotspots, selectedHotspot: hotspots[0] });

  wrapper.setProps({ hotspots: [mockRawHotspot()] });

  expect(wrapper.state().groupedHotspots).toHaveLength(1);
  expect(wrapper.state().groupedHotspots[0].categories).toHaveLength(1);
  expect(wrapper.state().groupedHotspots[0].categories[0].hotspots).toHaveLength(1);
});

it('should expand the categories for which the location is selected', () => {
  const wrapper = shallowRender({ hotspots, selectedHotspot: hotspots[0] });

  wrapper.setState({ expandedCategories: { cat1: true, cat2: false } });

  wrapper.setProps({ selectedHotspotLocation: 1 });

  expect(wrapper.state().expandedCategories).toEqual({ cat1: true, cat2: true });
});

function shallowRender(props: Partial<HotspotList['props']> = {}) {
  return shallow<HotspotList>(
    <HotspotList
      hotspots={[]}
      hotspotsTotal={0}
      isStaticListOfHotspots={false}
      loadingMore={false}
      onHotspotClick={jest.fn()}
      onLoadMore={jest.fn()}
      onLocationClick={jest.fn()}
      securityCategories={{}}
      selectedHotspot={mockRawHotspot({ key: 'h2' })}
      statusFilter={HotspotStatusFilter.TO_REVIEW}
      {...props}
    />
  );
}
