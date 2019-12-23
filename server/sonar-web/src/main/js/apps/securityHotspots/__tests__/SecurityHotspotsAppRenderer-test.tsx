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
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import { mockRawHotspot } from '../../../helpers/mocks/security-hotspots';
import { HotspotStatusFilter } from '../../../types/security-hotspots';
import SecurityHotspotsAppRenderer, {
  SecurityHotspotsAppRendererProps
} from '../SecurityHotspotsAppRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender()
      .find(ScreenPositionHelper)
      .dive()
  ).toMatchSnapshot();
});

it('should render correctly with hotspots', () => {
  const hotspots = [mockRawHotspot({ key: 'h1' }), mockRawHotspot({ key: 'h2' })];
  expect(
    shallowRender({ hotspots })
      .find(ScreenPositionHelper)
      .dive()
  ).toMatchSnapshot();
  expect(
    shallowRender({ hotspots, selectedHotspotKey: 'h2' })
      .find(ScreenPositionHelper)
      .dive()
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<SecurityHotspotsAppRendererProps> = {}) {
  return shallow(
    <SecurityHotspotsAppRenderer
      hotspots={[]}
      loading={false}
      onChangeFilters={jest.fn()}
      onHotspotClick={jest.fn()}
      onUpdateHotspot={jest.fn()}
      securityCategories={{}}
      filters={{ assignedToMe: false, status: HotspotStatusFilter.TO_REVIEW }}
      {...props}
    />
  );
}
