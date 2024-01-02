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
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockRawHotspot, mockStandards } from '../../../helpers/mocks/security-hotspots';
import { scrollToElement } from '../../../helpers/scrolling';
import { SecurityStandard } from '../../../types/security';
import { HotspotStatusFilter } from '../../../types/security-hotspots';
import SecurityHotspotsAppRenderer, {
  SecurityHotspotsAppRendererProps,
} from '../SecurityHotspotsAppRenderer';

jest.mock('../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn(),
}));

jest.mock('../../../components/common/ScreenPositionHelper');

beforeEach(() => {
  jest.clearAllMocks();
});

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useRef: jest.fn(),
    useEffect: jest.fn(),
  };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({
      filters: {
        assignedToMe: true,
        inNewCodePeriod: false,
        status: HotspotStatusFilter.TO_REVIEW,
      },
    })
  ).toMatchSnapshot('no hotspots with filters');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
});

it('should render correctly with hotspots', () => {
  const hotspots = [mockRawHotspot({ key: 'h1' }), mockRawHotspot({ key: 'h2' })];
  expect(shallowRender({ hotspots, hotspotsTotal: 2 })).toMatchSnapshot();
  expect(
    shallowRender({ hotspots, hotspotsTotal: 3, selectedHotspot: mockRawHotspot({ key: 'h2' }) })
      .find(ScreenPositionHelper)
      .dive()
  ).toMatchSnapshot();
});

it('should render correctly when filtered by category or cwe', () => {
  const hotspots = [mockRawHotspot({ key: 'h1' }), mockRawHotspot({ key: 'h2' })];

  expect(
    shallowRender({ filterByCWE: '327', hotspots, hotspotsTotal: 2, selectedHotspot: hotspots[0] })
      .find(ScreenPositionHelper)
      .dive()
  ).toMatchSnapshot('cwe');
  expect(
    shallowRender({
      filterByCategory: { category: 'a1', standard: SecurityStandard.OWASP_TOP10 },
      hotspots,
      hotspotsTotal: 2,
      selectedHotspot: hotspots[0],
    })
      .find(ScreenPositionHelper)
      .dive()
  ).toMatchSnapshot('category');
});

describe('side effect', () => {
  const fakeElement = document.createElement('span');
  const fakeParent = document.createElement('div');

  beforeEach(() => {
    (React.useEffect as jest.Mock).mockImplementationOnce((f) => f());
    jest.spyOn(document, 'querySelector').mockImplementationOnce(() => fakeElement);
    (React.useRef as jest.Mock).mockImplementationOnce(() => ({ current: fakeParent }));
  });

  it('should trigger scrolling', () => {
    shallowRender({ selectedHotspot: mockRawHotspot() });

    expect(scrollToElement).toHaveBeenCalledWith(
      fakeElement,
      expect.objectContaining({ parent: fakeParent })
    );
  });

  it('should not trigger scrolling if no selected hotspot', () => {
    shallowRender();
    expect(scrollToElement).not.toHaveBeenCalled();
  });

  it('should not trigger scrolling if no parent', () => {
    const mockUseRef = React.useRef as jest.Mock;
    mockUseRef.mockReset();
    mockUseRef.mockImplementationOnce(() => ({ current: null }));
    shallowRender({ selectedHotspot: mockRawHotspot() });
    expect(scrollToElement).not.toHaveBeenCalled();
  });
});

function shallowRender(props: Partial<SecurityHotspotsAppRendererProps> = {}) {
  return shallow(
    <SecurityHotspotsAppRenderer
      component={mockComponent()}
      filters={{
        assignedToMe: false,
        inNewCodePeriod: false,
        status: HotspotStatusFilter.TO_REVIEW,
      }}
      hotspots={[]}
      hotspotsTotal={0}
      isStaticListOfHotspots={true}
      loading={false}
      loadingMeasure={false}
      loadingMore={false}
      onChangeFilters={jest.fn()}
      onHotspotClick={jest.fn()}
      onLoadMore={jest.fn()}
      onSwitchStatusFilter={jest.fn()}
      onUpdateHotspot={jest.fn()}
      onLocationClick={jest.fn()}
      securityCategories={{}}
      selectedHotspot={undefined}
      standards={mockStandards()}
      {...props}
    />
  );
}
