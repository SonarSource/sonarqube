/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import { GlobalNav } from '../GlobalNav';
import { isSonarCloud } from '../../../../../helpers/system';

jest.mock('../../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

const appState: GlobalNav['props']['appState'] = {
  globalPages: [],
  canAdmin: false,
  organizationsEnabled: false,
  qualifiers: []
};
const location = { pathname: '' };

it('should render for SonarQube', () => {
  runTest(false);
});

it('should render for SonarCloud', () => {
  runTest(true);
});

function runTest(mockedIsSonarCloud: boolean) {
  (isSonarCloud as jest.Mock).mockImplementation(() => mockedIsSonarCloud);
  const wrapper = shallow(
    <GlobalNav
      appState={appState}
      currentUser={{ isLoggedIn: false }}
      location={location}
      suggestions={[]}
    />
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.setProps({ currentUser: { isLoggedIn: true } });
  expect(wrapper.find('GlobalNavPlus').exists()).toBe(true);
}
