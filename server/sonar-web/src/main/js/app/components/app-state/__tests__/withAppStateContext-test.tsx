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
import { mockAppState } from '../../../../helpers/testMocks';
import { AppState } from '../../../../types/appstate';
import withAppStateContext from '../withAppStateContext';

const appState = mockAppState();

jest.mock('../AppStateContext', () => {
  return {
    AppStateContext: {
      Consumer: ({ children }: { children: (props: {}) => React.ReactNode }) => {
        return children(appState);
      },
    },
  };
});

class Wrapped extends React.Component<{ appState: AppState }> {
  render() {
    return <div />;
  }
}

const UnderTest = withAppStateContext(Wrapped);

it('should inject appState', () => {
  const wrapper = shallow(<UnderTest />);
  expect(wrapper.dive().type()).toBe(Wrapped);
  expect(wrapper.dive<Wrapped>().props().appState).toEqual(appState);
});
