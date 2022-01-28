/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import RadioToggle from '../../../../components/controls/RadioToggle';
import SelectLegacy from '../../../../components/controls/SelectLegacy';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { HotspotStatusFilter } from '../../../../types/security-hotspots';
import { AssigneeFilterOption, FilterBar, FilterBarProps } from '../FilterBar';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('anonymous');
  expect(shallowRender({ currentUser: mockLoggedInUser() })).toMatchSnapshot('logged-in');
  expect(shallowRender({ onBranch: false })).toMatchSnapshot('on Pull request');
  expect(shallowRender({ hotspotsReviewedMeasure: '23.30' })).toMatchSnapshot(
    'with hotspots reviewed measure'
  );
  expect(
    shallowRender({
      currentUser: mockLoggedInUser(),
      component: mockComponent({ qualifier: ComponentQualifier.Application })
    })
  ).toMatchSnapshot('non-project');
});

it('should render correctly when the list of hotspot is static', () => {
  const onShowAllHotspots = jest.fn();

  const wrapper = shallowRender({
    isStaticListOfHotspots: true,
    onShowAllHotspots
  });
  expect(wrapper).toMatchSnapshot();

  wrapper.find('a').simulate('click');
  expect(onShowAllHotspots).toHaveBeenCalled();
});

it('should trigger onChange for status', () => {
  const onChangeFilters = jest.fn();
  const wrapper = shallowRender({ onChangeFilters });

  const { onChange } = wrapper
    .find(SelectLegacy)
    .at(0)
    .props();

  onChange!({ value: HotspotStatusFilter.SAFE });
  expect(onChangeFilters).toBeCalledWith({ status: HotspotStatusFilter.SAFE });
});

it('should trigger onChange for self-assigned toggle', () => {
  const onChangeFilters = jest.fn();
  const wrapper = shallowRender({ currentUser: mockLoggedInUser(), onChangeFilters });

  const { onCheck } = wrapper.find(RadioToggle).props();

  onCheck!(AssigneeFilterOption.ALL);
  expect(onChangeFilters).toBeCalledWith({ assignedToMe: false });
});

it('should trigger onChange for leak period', () => {
  const onChangeFilters = jest.fn();
  const wrapper = shallowRender({ onChangeFilters });

  const { onChange } = wrapper
    .find(SelectLegacy)
    .at(1)
    .props();

  onChange!({ value: true });
  expect(onChangeFilters).toBeCalledWith({ sinceLeakPeriod: true });
});

function shallowRender(props: Partial<FilterBarProps> = {}) {
  return shallow(
    <FilterBar
      component={mockComponent()}
      currentUser={mockCurrentUser()}
      filters={{
        assignedToMe: false,
        sinceLeakPeriod: false,
        status: HotspotStatusFilter.TO_REVIEW
      }}
      isStaticListOfHotspots={false}
      loadingMeasure={false}
      onBranch={true}
      onChangeFilters={jest.fn()}
      onShowAllHotspots={jest.fn()}
      {...props}
    />
  );
}
