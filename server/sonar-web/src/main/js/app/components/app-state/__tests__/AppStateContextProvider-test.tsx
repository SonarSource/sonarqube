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
import { mount } from 'enzyme';
import * as React from 'react';
import { mockAppState } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import AppStateContextProvider, { AppStateContextProviderProps } from '../AppStateContextProvider';

it('should set value correctly', async () => {
  const appState = mockAppState({ settings: { 'sonar.lf.logoUrl': 'whatevs/' } });
  const wrapper = render({
    appState,
  });
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
});

function render(override?: Partial<AppStateContextProviderProps>) {
  return mount(<AppStateContextProvider appState={mockAppState()} {...override} />);
}
