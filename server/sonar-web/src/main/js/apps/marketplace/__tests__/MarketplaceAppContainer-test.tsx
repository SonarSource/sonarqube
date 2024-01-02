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
import { mockAppState, mockLocation } from '../../../helpers/testMocks';
import { GlobalSettingKeys } from '../../../types/settings';
import { EditionKey } from '../../../types/editions';
import { MarketplaceAppContainer, MarketplaceAppContainerProps } from '../MarketplaceAppContainer';

it('should render correctly', () => {
  expect(shallowRender().dive()).toMatchSnapshot('default');
  expect(
    shallowRender({
      appState: mockAppState({
        settings: {
          [GlobalSettingKeys.UpdatecenterActivated]: 'true',
        },
      }),
    }).dive()
  ).toMatchSnapshot('update center active');
});

function shallowRender(overrides: Partial<MarketplaceAppContainerProps> = {}) {
  return shallow<MarketplaceAppContainerProps>(
    <MarketplaceAppContainer
      appState={mockAppState({ edition: EditionKey.community, standalone: true })}
      location={mockLocation()}
      {...overrides}
    />
  );
}
