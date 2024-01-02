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
import ScreenPositionHelper from '../../../../components/common/ScreenPositionHelper';
import { mockDefinition } from '../../../../helpers/mocks/settings';
import { mockLocation } from '../../../../helpers/testMocks';
import {
  ALM_INTEGRATION_CATEGORY,
  ANALYSIS_SCOPE_CATEGORY,
  LANGUAGES_CATEGORY,
  NEW_CODE_PERIOD_CATEGORY,
  PULL_REQUEST_DECORATION_BINDING_CATEGORY,
} from '../../constants';
import { SettingsAppRenderer, SettingsAppRendererProps } from '../SettingsAppRenderer';

it('should render loading correctly', () => {
  expect(shallowRender({ loading: true }).type()).toBeNull();
});

it('should render default view correctly', () => {
  const wrapper = shallowRender();

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find(ScreenPositionHelper).dive()).toMatchSnapshot('All Categories List');
});

it.each([
  [NEW_CODE_PERIOD_CATEGORY],
  [LANGUAGES_CATEGORY],
  [ANALYSIS_SCOPE_CATEGORY],
  [ALM_INTEGRATION_CATEGORY],
  [PULL_REQUEST_DECORATION_BINDING_CATEGORY],
])('should render %s correctly', (category) => {
  const wrapper = shallowRender({
    location: mockLocation({ query: { category } }),
  });

  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<SettingsAppRendererProps> = {}) {
  const definitions = [mockDefinition(), mockDefinition({ key: 'bar', category: 'general' })];
  return shallow(
    <SettingsAppRenderer
      definitions={definitions}
      loading={false}
      location={mockLocation()}
      {...props}
    />
  );
}
