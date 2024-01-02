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
import { getMeasures } from '../../../api/measures';
import { getSecurityHotspotList, getSecurityHotspots } from '../../../api/security-hotspots';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { mockBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockRawHotspot, mockStandards } from '../../../helpers/mocks/security-hotspots';
import { getStandards } from '../../../helpers/security-standard';
import {
  mockCurrentUser,
  mockFlowLocation,
  mockLocation,
  mockLoggedInUser,
} from '../../../helpers/testMocks';
import { mockEvent, waitAndUpdate } from '../../../helpers/testUtils';
import { SecurityStandard } from '../../../types/security';
import {
  HotspotResolution,
  HotspotStatus,
  HotspotStatusFilter,
} from '../../../types/security-hotspots';
import { SecurityHotspotsApp } from '../SecurityHotspotsApp';
import SecurityHotspotsAppRenderer from '../SecurityHotspotsAppRenderer';

beforeEach(() => {
  jest.clearAllMocks();
});

jest.mock('../../../api/measures', () => ({
  getMeasures: jest.fn().mockResolvedValue([]),
}));

jest.mock('../../../api/security-hotspots', () => ({
  getSecurityHotspots: jest.fn().mockResolvedValue({ hotspots: [], paging: { total: 0 } }),
  getSecurityHotspotList: jest.fn().mockResolvedValue({ hotspots: [], rules: [] }),
}));

jest.mock('../../../helpers/security-standard', () => ({
  getStandards: jest.fn().mockResolvedValue({ sonarsourceSecurity: { cat1: { title: 'cat 1' } } }),
}));

jest.mock('../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn(),
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
      total: 1,
    },
  });
  (getMeasures as jest.Mock).mockResolvedValue([{ value: '86.6' }]);

  const wrapper = shallowRender();

  expect(wrapper.state().loading).toBe(true);
  expect(wrapper.state().loadingMeasure).toBe(true);

  expect(getStandards).toHaveBeenCalled();
  expect(getSecurityHotspots).toHaveBeenCalledWith(
    expect.objectContaining({
      branch: branch.name,
    })
  );
  expect(getMeasures).toHaveBeenCalledWith(
    expect.objectContaining({
      branch: branch.name,
    })
  );

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().hotspots).toEqual(hotspots);
  expect(wrapper.state().selectedHotspot).toBe(hotspots[0]);
  expect(wrapper.state().standards).toEqual({
    sonarsourceSecurity: {
      cat1: { title: 'cat 1' },
    },
  });
  expect(wrapper.state().loadingMeasure).toBe(false);
  expect(wrapper.state().hotspotsReviewedMeasure).toBe('86.6');
});

it('should handle category request', () => {
  (getStandards as jest.Mock).mockResolvedValue(mockStandards());
  (getMeasures as jest.Mock).mockResolvedValue([{ value: '86.6' }]);

  shallowRender({
    location: mockLocation({ query: { [SecurityStandard.OWASP_TOP10]: 'a1' } }),
  });

  expect(getSecurityHotspots).toHaveBeenCalledWith(
    expect.objectContaining({ [SecurityStandard.OWASP_TOP10]: 'a1' })
  );
});

it('should handle cwe request', () => {
  (getStandards as jest.Mock).mockResolvedValue(mockStandards());
  (getMeasures as jest.Mock).mockResolvedValue([{ value: '86.6' }]);

  shallowRender({
    location: mockLocation({ query: { [SecurityStandard.CWE]: '1004' } }),
  });

  expect(getSecurityHotspots).toHaveBeenCalledWith(
    expect.objectContaining({ [SecurityStandard.CWE]: '1004' })
  );
});

it('should handle file request', () => {
  (getStandards as jest.Mock).mockResolvedValue(mockStandards());
  (getMeasures as jest.Mock).mockResolvedValue([{ value: '86.6' }]);

  const filepath = 'src/path/to/file.java';

  shallowRender({
    location: mockLocation({ query: { files: filepath } }),
  });

  expect(getSecurityHotspots).toHaveBeenCalledWith(expect.objectContaining({ files: filepath }));
});

it('should load data correctly when hotspot key list is forced', async () => {
  const hotspots = [
    mockRawHotspot({ key: 'test1' }),
    mockRawHotspot({ key: 'test2' }),
    mockRawHotspot({ key: 'test3' }),
  ];
  const hotspotKeys = hotspots.map((h) => h.key);
  (getSecurityHotspotList as jest.Mock).mockResolvedValueOnce({
    hotspots,
  });

  const location = mockLocation({ query: { hotspots: hotspotKeys.join() } });
  const wrapper = shallowRender({
    location,
  });

  await waitAndUpdate(wrapper);
  expect(getSecurityHotspotList).toHaveBeenCalledWith(hotspotKeys, {
    projectKey: 'my-project',
    branch: 'branch-6.7',
  });
  expect(wrapper.state().hotspotKeys).toEqual(hotspotKeys);
  expect(wrapper.find(SecurityHotspotsAppRenderer).props().isStaticListOfHotspots).toBe(true);

  // Reset
  (getSecurityHotspots as jest.Mock).mockClear();
  (getSecurityHotspotList as jest.Mock).mockClear();

  // Simulate a new location
  wrapper.setProps({
    location: { ...location, query: { ...location.query, hotspots: undefined } },
  });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().hotspotKeys).toBeUndefined();
  expect(getSecurityHotspotList).not.toHaveBeenCalled();
  expect(getSecurityHotspots).toHaveBeenCalled();
});

it('should set "leakperiod" filter according to context (branchlike & location query)', () => {
  expect(shallowRender().state().filters.inNewCodePeriod).toBe(false);
  expect(shallowRender({ branchLike: mockPullRequest() }).state().filters.inNewCodePeriod).toBe(
    true
  );
  expect(
    shallowRender({ location: mockLocation({ query: { inNewCodePeriod: 'true' } }) }).state()
      .filters.inNewCodePeriod
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
      currentUser: mockLoggedInUser(),
    }).state().filters.assignedToMe
  ).toBe(true);
});

it('should handle loading more', async () => {
  const hotspots = [mockRawHotspot({ key: '1' }), mockRawHotspot({ key: '2' })];
  const hotspots2 = [mockRawHotspot({ key: '3' }), mockRawHotspot({ key: '4' })];
  (getSecurityHotspots as jest.Mock)
    .mockResolvedValueOnce({
      hotspots,
      paging: { total: 5 },
    })
    .mockResolvedValueOnce({
      hotspots: hotspots2,
      paging: { total: 5 },
    });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  wrapper.instance().handleLoadMore();

  expect(wrapper.state().loadingMore).toBe(true);
  expect(getSecurityHotspots).toHaveBeenCalledTimes(2);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loadingMore).toBe(false);
  expect(wrapper.state().hotspotsPageIndex).toBe(2);
  expect(wrapper.state().hotspotsTotal).toBe(5);
  expect(wrapper.state().hotspots).toHaveLength(4);
});

it('should handle hotspot update', async () => {
  const key = 'hotspotKey';
  const hotspots = [mockRawHotspot(), mockRawHotspot({ key })];
  const fetchBranchStatusMock = jest.fn();
  const branchLike = mockPullRequest();
  const componentKey = 'test';

  (getSecurityHotspots as jest.Mock).mockResolvedValueOnce({
    hotspots,
    paging: { pageIndex: 1, total: 1252 },
  });

  let wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({ hotspotsPageIndex: 2 });

  jest.clearAllMocks();
  (getSecurityHotspots as jest.Mock)
    .mockResolvedValueOnce({
      hotspots: [mockRawHotspot()],
      paging: { pageIndex: 1, total: 1251 },
    })
    .mockResolvedValueOnce({
      hotspots: [mockRawHotspot()],
      paging: { pageIndex: 2, total: 1251 },
    });

  const selectedHotspotIndex = wrapper
    .state()
    .hotspots.findIndex((h) => h.key === wrapper.state().selectedHotspot?.key);

  await wrapper.find(SecurityHotspotsAppRenderer).props().onUpdateHotspot(key);

  expect(getSecurityHotspots).toHaveBeenCalledTimes(2);

  expect(wrapper.state().hotspots).toHaveLength(2);
  expect(wrapper.state().hotspotsPageIndex).toBe(2);
  expect(wrapper.state().hotspotsTotal).toBe(1251);
  expect(
    wrapper.state().hotspots.findIndex((h) => h.key === wrapper.state().selectedHotspot?.key)
  ).toBe(selectedHotspotIndex);

  expect(getMeasures).toHaveBeenCalled();

  (getSecurityHotspots as jest.Mock).mockResolvedValueOnce({
    hotspots,
    paging: { pageIndex: 1, total: 1252 },
  });

  wrapper = shallowRender({
    branchLike,
    fetchBranchStatus: fetchBranchStatusMock,
    component: mockComponent({ key: componentKey }),
  });
  await wrapper.find(SecurityHotspotsAppRenderer).props().onUpdateHotspot(key);
  expect(fetchBranchStatusMock).toHaveBeenCalledWith(branchLike, componentKey);
});

it('should handle status filter change', async () => {
  const hotspots = [mockRawHotspot({ key: 'key1' })];
  const hotspots2 = [mockRawHotspot({ key: 'key2' })];
  (getSecurityHotspots as jest.Mock)
    .mockResolvedValueOnce({ hotspots, paging: { total: 1 } })
    .mockResolvedValueOnce({ hotspots: hotspots2, paging: { total: 1 } })
    .mockResolvedValueOnce({ hotspots: [], paging: { total: 0 } });

  const wrapper = shallowRender();

  expect(getSecurityHotspots).toHaveBeenCalledWith(
    expect.objectContaining({ status: HotspotStatus.TO_REVIEW, resolution: undefined })
  );

  await waitAndUpdate(wrapper);

  expect(getMeasures).toHaveBeenCalledTimes(1);

  // Set filter to SAFE:
  wrapper.instance().handleChangeFilters({ status: HotspotStatusFilter.SAFE });
  expect(getMeasures).toHaveBeenCalledTimes(1);

  expect(getSecurityHotspots).toHaveBeenCalledWith(
    expect.objectContaining({ status: HotspotStatus.REVIEWED, resolution: HotspotResolution.SAFE })
  );

  await waitAndUpdate(wrapper);

  expect(wrapper.state().hotspots[0]).toBe(hotspots2[0]);

  // Set filter to FIXED (use the other method to check this one):
  wrapper.instance().handleChangeStatusFilter(HotspotStatusFilter.FIXED);

  expect(getSecurityHotspots).toHaveBeenCalledWith(
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

  expect(getSecurityHotspots).toHaveBeenCalledWith(
    expect.objectContaining({ status: HotspotStatus.TO_REVIEW, resolution: undefined })
  );

  await waitAndUpdate(wrapper);

  expect(getMeasures).toHaveBeenCalledTimes(1);

  wrapper.instance().handleChangeFilters({ inNewCodePeriod: true });

  expect(getMeasures).toHaveBeenCalledTimes(2);
  expect(getSecurityHotspots).toHaveBeenCalledWith(
    expect.objectContaining({ inNewCodePeriod: true })
  );
});

it('should handle hotspot click', () => {
  const wrapper = shallowRender();
  const selectedHotspot = mockRawHotspot();
  wrapper.instance().handleHotspotClick(selectedHotspot);

  expect(wrapper.instance().state.selectedHotspotLocationIndex).toBeUndefined();
  expect(wrapper.instance().state.selectedHotspot).toEqual(selectedHotspot);
});

it('should handle secondary location click', () => {
  const wrapper = shallowRender();
  wrapper.instance().handleLocationClick(0);
  expect(wrapper.instance().state.selectedHotspotLocationIndex).toEqual(0);

  wrapper.instance().handleLocationClick(1);
  expect(wrapper.instance().state.selectedHotspotLocationIndex).toEqual(1);

  wrapper.instance().handleLocationClick(1);
  expect(wrapper.instance().state.selectedHotspotLocationIndex).toBeUndefined();

  wrapper.setState({ selectedHotspotLocationIndex: 2 });
  wrapper.instance().handleLocationClick();
  expect(wrapper.instance().state.selectedHotspotLocationIndex).toBeUndefined();
});

describe('keyboard navigation', () => {
  const hotspots = [
    mockRawHotspot({ key: 'k1' }),
    mockRawHotspot({ key: 'k2' }),
    mockRawHotspot({ key: 'k3' }),
  ];
  const flowsData = {
    flows: [{ locations: [mockFlowLocation(), mockFlowLocation(), mockFlowLocation()] }],
  };
  const hotspotsForLocation = mockRawHotspot(flowsData);

  (getSecurityHotspots as jest.Mock).mockResolvedValueOnce({ hotspots, paging: { total: 3 } });

  const wrapper = shallowRender();

  it.each([
    ['selecting next', 0, 1, 1],
    ['selecting previous', 1, -1, 0],
    ['selecting previous, non-existent', 0, -1, 0],
    ['selecting next, non-existent', 2, 1, 2],
    ['jumping down', 0, 18, 2],
    ['jumping up', 2, -18, 0],
    ['none selected', 4, -2, 4],
  ])('should work when %s', (_, start, shift, expected) => {
    wrapper.setState({ selectedHotspot: hotspots[start] });
    wrapper.instance().selectNeighboringHotspot(shift);

    expect(wrapper.state().selectedHotspot).toBe(hotspots[expected]);
  });

  it.each([
    ['selecting next locations when nothing is selected', undefined, 0],
    ['selecting next locations', 0, 1],
    ['selecting next locations, non-existent', 2, undefined],
  ])('should work when %s', (_, start, expected) => {
    wrapper.setState({ selectedHotspotLocationIndex: start, selectedHotspot: hotspotsForLocation });
    wrapper.instance().handleKeyDown(mockEvent({ altKey: true, key: KeyboardKeys.DownArrow }));

    expect(wrapper.state().selectedHotspotLocationIndex).toBe(expected);
  });

  it.each([
    ['selecting previous locations when nothing is selected', undefined, undefined],
    ['selecting previous locations', 1, 0],
    ['selecting previous locations, non-existent', 0, undefined],
  ])('should work when %s', (_, start, expected) => {
    wrapper.setState({ selectedHotspotLocationIndex: start, selectedHotspot: hotspotsForLocation });
    wrapper.instance().handleKeyDown(mockEvent({ altKey: true, key: KeyboardKeys.UpArrow }));

    expect(wrapper.state().selectedHotspotLocationIndex).toBe(expected);
  });

  it('should not change location index when locations are empty', () => {
    wrapper.setState({ selectedHotspotLocationIndex: undefined, selectedHotspot: hotspots[0] });

    wrapper.instance().handleKeyDown(mockEvent({ altKey: true, key: KeyboardKeys.UpArrow }));
    expect(wrapper.state().selectedHotspotLocationIndex).toBeUndefined();

    wrapper.instance().handleKeyDown(mockEvent({ altKey: true, key: KeyboardKeys.DownArrow }));
    expect(wrapper.state().selectedHotspotLocationIndex).toBeUndefined();
  });
});

function shallowRender(props: Partial<SecurityHotspotsApp['props']> = {}) {
  return shallow<SecurityHotspotsApp>(
    <SecurityHotspotsApp
      fetchBranchStatus={jest.fn()}
      branchLike={branch}
      component={mockComponent()}
      currentUser={mockCurrentUser()}
      location={mockLocation()}
      {...props}
    />
  );
}
