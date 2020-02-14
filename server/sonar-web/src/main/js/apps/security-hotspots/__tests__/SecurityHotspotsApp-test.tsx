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
import { getMeasures } from '../../../api/measures';
import { getSecurityHotspotList, getSecurityHotspots } from '../../../api/security-hotspots';
import { mockBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import { mockRawHotspot } from '../../../helpers/mocks/security-hotspots';
import { getStandards } from '../../../helpers/security-standard';
import {
  mockComponent,
  mockCurrentUser,
  mockLocation,
  mockLoggedInUser,
  mockRouter
} from '../../../helpers/testMocks';
import {
  HotspotResolution,
  HotspotStatus,
  HotspotStatusFilter
} from '../../../types/security-hotspots';
import { SecurityHotspotsApp } from '../SecurityHotspotsApp';
import SecurityHotspotsAppRenderer from '../SecurityHotspotsAppRenderer';

beforeEach(() => jest.clearAllMocks());

jest.mock('sonar-ui-common/helpers/pages', () => ({
  addNoFooterPageClass: jest.fn(),
  removeNoFooterPageClass: jest.fn()
}));

jest.mock('../../../api/measures', () => ({
  getMeasures: jest.fn().mockResolvedValue([])
}));

jest.mock('../../../api/security-hotspots', () => ({
  getSecurityHotspots: jest.fn().mockResolvedValue({ hotspots: [], paging: { total: 0 } }),
  getSecurityHotspotList: jest.fn().mockResolvedValue({ hotspots: [], rules: [] })
}));

jest.mock('../../../helpers/security-standard', () => ({
  getStandards: jest.fn().mockResolvedValue({ sonarsourceSecurity: { cat1: { title: 'cat 1' } } })
}));

const branch = mockBranch();

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should load data correctly', async () => {
  const hotspots = [mockRawHotspot()];
  (getSecurityHotspots as jest.Mock).mockResolvedValue({
    hotspots,
    paging: {
      total: 1
    }
  });
  (getMeasures as jest.Mock).mockResolvedValue([{ value: '86.6' }]);

  const wrapper = shallowRender();

  expect(wrapper.state().loading).toBe(true);
  expect(wrapper.state().loadingMeasure).toBe(true);

  expect(addNoFooterPageClass).toBeCalled();
  expect(getStandards).toBeCalled();
  expect(getSecurityHotspots).toBeCalledWith(
    expect.objectContaining({
      branch: branch.name
    })
  );
  expect(getMeasures).toBeCalledWith(
    expect.objectContaining({
      branch: branch.name
    })
  );

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().hotspots).toEqual(hotspots);
  expect(wrapper.state().selectedHotspot).toBe(hotspots[0]);
  expect(wrapper.state().securityCategories).toEqual({
    cat1: { title: 'cat 1' }
  });
  expect(wrapper.state().loadingMeasure).toBe(false);
  expect(wrapper.state().hotspotsReviewedMeasure).toBe('86.6');
});

it('should load data correctly when hotspot key list is forced', async () => {
  const hotspots = [
    mockRawHotspot({ key: 'test1' }),
    mockRawHotspot({ key: 'test2' }),
    mockRawHotspot({ key: 'test3' })
  ];
  const hotspotKeys = hotspots.map(h => h.key);
  (getSecurityHotspotList as jest.Mock).mockResolvedValueOnce({
    hotspots
  });

  const location = mockLocation({ query: { hotspots: hotspotKeys.join() } });
  const router = mockRouter();
  const wrapper = shallowRender({
    location,
    router
  });

  await waitAndUpdate(wrapper);
  expect(getSecurityHotspotList).toBeCalledWith(hotspotKeys, {
    projectKey: 'my-project',
    branch: 'branch-6.7'
  });
  expect(wrapper.state().hotspotKeys).toEqual(hotspotKeys);
  expect(wrapper.find(SecurityHotspotsAppRenderer).props().isStaticListOfHotspots).toBeTruthy();

  // Reset
  (getSecurityHotspots as jest.Mock).mockClear();
  (getSecurityHotspotList as jest.Mock).mockClear();
  wrapper
    .find(SecurityHotspotsAppRenderer)
    .props()
    .onShowAllHotspots();
  expect(router.push).toHaveBeenCalledWith({
    ...location,
    query: { ...location.query, hotspots: undefined }
  });

  // Simulate a new location
  wrapper.setProps({
    location: { ...location, query: { ...location.query, hotspots: undefined } }
  });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().hotspotKeys).toBeUndefined();
  expect(getSecurityHotspotList).not.toHaveBeenCalled();
  expect(getSecurityHotspots).toHaveBeenCalled();
});

it('should set "leakperiod" filter according to context (branchlike & location query)', () => {
  expect(shallowRender().state().filters.sinceLeakPeriod).toBe(false);
  expect(shallowRender({ branchLike: mockPullRequest() }).state().filters.sinceLeakPeriod).toBe(
    true
  );
  expect(
    shallowRender({ location: mockLocation({ query: { sinceLeakPeriod: 'true' } }) }).state()
      .filters.sinceLeakPeriod
  ).toBe(true);
});

it('should set "assigned to me" filter according to context (logged in & explicit location query)', () => {
  const wrapper = shallowRender();
  expect(wrapper.state().filters.assignedToMe).toBe(false);

  wrapper.setProps({ location: mockLocation({ query: { assignedToMe: 'true' } }) });
  expect(wrapper.state().filters.assignedToMe).toBe(false);

  expect(shallowRender({ currentUser: mockLoggedInUser() }).state().filters.assignedToMe).toBe(
    false
  );
  expect(
    shallowRender({
      location: mockLocation({ query: { assignedToMe: 'true' } }),
      currentUser: mockLoggedInUser()
    }).state().filters.assignedToMe
  ).toBe(true);
});

it('should handle loading more', async () => {
  const hotspots = [mockRawHotspot({ key: '1' }), mockRawHotspot({ key: '2' })];
  const hotspots2 = [mockRawHotspot({ key: '3' }), mockRawHotspot({ key: '4' })];
  (getSecurityHotspots as jest.Mock)
    .mockResolvedValueOnce({
      hotspots,
      paging: { total: 5 }
    })
    .mockResolvedValueOnce({
      hotspots: hotspots2,
      paging: { total: 5 }
    });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  wrapper.instance().handleLoadMore();

  expect(wrapper.state().loadingMore).toBe(true);
  expect(getSecurityHotspots).toBeCalledTimes(2);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loadingMore).toBe(false);
  expect(wrapper.state().hotspotsPageIndex).toBe(2);
  expect(wrapper.state().hotspotsTotal).toBe(5);
  expect(wrapper.state().hotspots).toHaveLength(4);
});

it('should handle hotspot update', async () => {
  const key = 'hotspotKey';
  const hotspots = [mockRawHotspot(), mockRawHotspot({ key })];
  (getSecurityHotspots as jest.Mock).mockResolvedValueOnce({
    hotspots,
    paging: { pageIndex: 1, total: 1252 }
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({ hotspotsPageIndex: 2 });

  jest.clearAllMocks();
  (getSecurityHotspots as jest.Mock)
    .mockResolvedValueOnce({
      hotspots: [mockRawHotspot()],
      paging: { pageIndex: 1, total: 1251 }
    })
    .mockResolvedValueOnce({
      hotspots: [mockRawHotspot()],
      paging: { pageIndex: 2, total: 1251 }
    });

  const selectedHotspotIndex = wrapper
    .state()
    .hotspots.findIndex(h => h.key === wrapper.state().selectedHotspot?.key);

  await wrapper
    .find(SecurityHotspotsAppRenderer)
    .props()
    .onUpdateHotspot(key);

  expect(getSecurityHotspots).toHaveBeenCalledTimes(2);

  expect(wrapper.state().hotspots).toHaveLength(2);
  expect(wrapper.state().hotspotsPageIndex).toBe(2);
  expect(wrapper.state().hotspotsTotal).toBe(1251);
  expect(
    wrapper.state().hotspots.findIndex(h => h.key === wrapper.state().selectedHotspot?.key)
  ).toBe(selectedHotspotIndex);

  expect(getMeasures).toBeCalled();
});

it('should handle status filter change', async () => {
  const hotspots = [mockRawHotspot({ key: 'key1' })];
  const hotspots2 = [mockRawHotspot({ key: 'key2' })];
  (getSecurityHotspots as jest.Mock)
    .mockResolvedValueOnce({ hotspots, paging: { total: 1 } })
    .mockResolvedValueOnce({ hotspots: hotspots2, paging: { total: 1 } })
    .mockResolvedValueOnce({ hotspots: [], paging: { total: 0 } });

  const wrapper = shallowRender();

  expect(getSecurityHotspots).toBeCalledWith(
    expect.objectContaining({ status: HotspotStatus.TO_REVIEW, resolution: undefined })
  );

  await waitAndUpdate(wrapper);

  expect(getMeasures).toBeCalledTimes(1);

  // Set filter to SAFE:
  wrapper.instance().handleChangeFilters({ status: HotspotStatusFilter.SAFE });
  expect(getMeasures).toBeCalledTimes(1);

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

it('should handle leakPeriod filter change', async () => {
  const hotspots = [mockRawHotspot({ key: 'key1' })];
  const hotspots2 = [mockRawHotspot({ key: 'key2' })];
  (getSecurityHotspots as jest.Mock)
    .mockResolvedValueOnce({ hotspots, paging: { total: 1 } })
    .mockResolvedValueOnce({ hotspots: hotspots2, paging: { total: 1 } })
    .mockResolvedValueOnce({ hotspots: [], paging: { total: 0 } });

  const wrapper = shallowRender();

  expect(getSecurityHotspots).toBeCalledWith(
    expect.objectContaining({ status: HotspotStatus.TO_REVIEW, resolution: undefined })
  );

  await waitAndUpdate(wrapper);

  expect(getMeasures).toBeCalledTimes(1);

  wrapper.instance().handleChangeFilters({ sinceLeakPeriod: true });

  expect(getMeasures).toBeCalledTimes(2);
  expect(getSecurityHotspots).toBeCalledWith(expect.objectContaining({ sinceLeakPeriod: true }));
});

function shallowRender(props: Partial<SecurityHotspotsApp['props']> = {}) {
  return shallow<SecurityHotspotsApp>(
    <SecurityHotspotsApp
      branchLike={branch}
      component={mockComponent()}
      currentUser={mockCurrentUser()}
      location={mockLocation()}
      router={mockRouter()}
      {...props}
    />
  );
}
