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
import { HotspotStatusFilter, RiskExposure } from '../../../../types/security-hotspots';
import HotspotList, { HotspotListProps } from '../HotspotList';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ loadingMore: true })).toMatchSnapshot();
});

it('should render correctly when the list of hotspot is static', () => {
  expect(shallowRender({ isStaticListOfHotspots: true })).toMatchSnapshot();
});

it('should render correctly with hotspots', () => {
  const hotspots = [
    mockRawHotspot({ key: 'h1', securityCategory: 'cat2' }),
    mockRawHotspot({ key: 'h2', securityCategory: 'cat1' }),
    mockRawHotspot({
      key: 'h3',
      securityCategory: 'cat1',
      vulnerabilityProbability: RiskExposure.MEDIUM
    }),
    mockRawHotspot({
      key: 'h4',
      securityCategory: 'cat1',
      vulnerabilityProbability: RiskExposure.MEDIUM
    }),
    mockRawHotspot({
      key: 'h5',
      securityCategory: 'cat2',
      vulnerabilityProbability: RiskExposure.MEDIUM
    })
  ];
  expect(shallowRender({ hotspots })).toMatchSnapshot('no pagination');
  expect(shallowRender({ hotspots, hotspotsTotal: 7 })).toMatchSnapshot('pagination');
});

function shallowRender(props: Partial<HotspotListProps> = {}) {
  return shallow(
    <HotspotList
      hotspots={[]}
      isStaticListOfHotspots={false}
      loadingMore={false}
      onHotspotClick={jest.fn()}
      onLoadMore={jest.fn()}
      securityCategories={{}}
      selectedHotspotKey="h2"
      statusFilter={HotspotStatusFilter.TO_REVIEW}
      {...props}
    />
  );
}
