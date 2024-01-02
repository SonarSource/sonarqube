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
import { mockAppState } from '../../../helpers/testMocks';
import { GlobalSettingKeys } from '../../../types/settings';
import { MetricKey } from '../../../types/metrics';
import { RatingTooltipContent, RatingTooltipContentProps } from '../RatingTooltipContent';

it('should render maintainability correctly', () => {
  expect(shallowRender()).toMatchSnapshot('sqale rating');
  expect(shallowRender({ value: 1 })).toMatchSnapshot('sqale rating A');
  expect(shallowRender({ appState: mockAppState({ settings: {} }) })).toMatchSnapshot(
    'sqale rating default grid'
  );
  expect(
    shallowRender({
      appState: mockAppState({ settings: { [GlobalSettingKeys.RatingGrid]: '0,0.1' } }),
    })
  ).toMatchSnapshot('sqale rating wrong grid');
});

it('should render other ratings correctly', () => {
  expect(shallowRender({ metricKey: MetricKey.security_rating })).toMatchSnapshot(
    'security rating'
  );
  expect(shallowRender({ metricKey: MetricKey.new_security_rating })).toMatchSnapshot(
    'new security rating'
  );
});

it('should ignore non-rating metrics', () => {
  expect(shallowRender({ metricKey: MetricKey.code_smells }).type()).toBeNull();
});

function shallowRender(overrides: Partial<RatingTooltipContentProps> = {}) {
  return shallow(
    <RatingTooltipContent
      appState={mockAppState({ settings: { [GlobalSettingKeys.RatingGrid]: '0.05,0.1,0.2,0.4' } })}
      metricKey={MetricKey.sqale_rating}
      value={2}
      {...overrides}
    />
  );
}
