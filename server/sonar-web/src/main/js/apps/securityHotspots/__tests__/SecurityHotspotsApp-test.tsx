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
import { addNoFooterPageClass } from 'sonar-ui-common/helpers/pages';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getSecurityHotspots } from '../../../api/security-hotspots';
import { mockBranch } from '../../../helpers/mocks/branch-like';
import { mockRawHotspot } from '../../../helpers/mocks/security-hotspots';
import { getStandards } from '../../../helpers/security-standard';
import { mockComponent, mockCurrentUser } from '../../../helpers/testMocks';
import {
  HotspotResolution,
  HotspotStatus,
  HotspotStatusFilter
} from '../../../types/security-hotspots';
import { SecurityHotspotsApp } from '../SecurityHotspotsApp';
import SecurityHotspotsAppRenderer from '../SecurityHotspotsAppRenderer';

jest.mock('sonar-ui-common/helpers/pages', () => ({
  addNoFooterPageClass: jest.fn(),
  removeNoFooterPageClass: jest.fn()
}));

jest.mock('../../../api/security-hotspots', () => ({
  getSecurityHotspots: jest.fn().mockResolvedValue({ hotspots: [], rules: [] })
}));

jest.mock('../../../helpers/security-standard', () => ({
  getStandards: jest.fn()
}));

const branch = mockBranch();

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should load data correctly', async () => {
  const sonarsourceSecurity = { cat1: { title: 'cat 1' } };
  (getStandards as jest.Mock).mockResolvedValue({ sonarsourceSecurity });

  const hotspots = [mockRawHotspot()];
  (getSecurityHotspots as jest.Mock).mockResolvedValueOnce({
    hotspots
  });

  const wrapper = shallowRender();

  expect(wrapper.state().loading).toBe(true);

  expect(addNoFooterPageClass).toBeCalled();
  expect(getStandards).toBeCalled();
  expect(getSecurityHotspots).toBeCalledWith(
    expect.objectContaining({
      branch: branch.name
    })
  );

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().hotspots).toEqual(hotspots);
  expect(wrapper.state().selectedHotspotKey).toBe(hotspots[0].key);
  expect(wrapper.state().securityCategories).toBe(sonarsourceSecurity);

  expect(wrapper.state());
});

it('should handle hotspot update', async () => {
  const key = 'hotspotKey';
  const hotspots = [mockRawHotspot(), mockRawHotspot({ key })];
  (getSecurityHotspots as jest.Mock).mockResolvedValue({
    hotspots
  });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  wrapper
    .find(SecurityHotspotsAppRenderer)
    .props()
    .onUpdateHotspot({ key, status: HotspotStatus.REVIEWED, resolution: HotspotResolution.SAFE });

  expect(wrapper.state().hotspots[0]).toEqual(hotspots[0]);
  expect(wrapper.state().hotspots[1]).toEqual({
    ...hotspots[1],
    status: HotspotStatus.REVIEWED,
    resolution: HotspotResolution.SAFE
  });
});

it('should handle status filter change', async () => {
  const hotspots = [mockRawHotspot({ key: 'key1' })];
  const hotspots2 = [mockRawHotspot({ key: 'key2' })];
  (getSecurityHotspots as jest.Mock)
    .mockResolvedValueOnce({ hotspots })
    .mockResolvedValueOnce({ hotspots: hotspots2 })
    .mockResolvedValueOnce({ hotspots: [] });

  const wrapper = shallowRender();

  expect(getSecurityHotspots).toBeCalledWith(
    expect.objectContaining({ status: HotspotStatus.TO_REVIEW, resolution: undefined })
  );

  await waitAndUpdate(wrapper);

  // Set filter to SAFE:
  wrapper.instance().handleChangeFilters({ status: HotspotStatusFilter.SAFE });

  expect(getSecurityHotspots).toBeCalledWith(
    expect.objectContaining({ status: HotspotStatus.REVIEWED, resolution: HotspotResolution.SAFE })
  );

  await waitAndUpdate(wrapper);

  expect(wrapper.state().hotspots[0]).toBe(hotspots2[0]);

  // Set filter to FIXED
  wrapper.instance().handleChangeFilters({ status: HotspotStatusFilter.FIXED });

  expect(getSecurityHotspots).toBeCalledWith(
    expect.objectContaining({ status: HotspotStatus.REVIEWED, resolution: HotspotResolution.FIXED })
  );

  await waitAndUpdate(wrapper);

  expect(wrapper.state().hotspots).toHaveLength(0);
});

function shallowRender(props: Partial<SecurityHotspotsApp['props']> = {}) {
  return shallow<SecurityHotspotsApp>(
    <SecurityHotspotsApp
      branchLike={branch}
      component={mockComponent()}
      currentUser={mockCurrentUser()}
      {...props}
    />
  );
}
