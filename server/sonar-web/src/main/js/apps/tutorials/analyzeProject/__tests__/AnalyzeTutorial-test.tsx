/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import AnalyzeTutorial from '../AnalyzeTutorial';

jest.mock('../../../../helpers/system', () => ({
  isSonarCloud: jest.fn().mockReturnValue(true)
}));

Date.now = jest.fn().mockReturnValue(1540457859031);

const component = {
  key: 'foo',
  analysisDate: '2016-01-01',
  breadcrumbs: [],
  name: 'Foo',
  organization: 'org',
  qualifier: 'TRK',
  version: '0.0.1'
};

const loggedInUser: T.LoggedInUser = {
  groups: [],
  isLoggedIn: true,
  login: 'luke',
  name: 'Skywalker',
  scmAccounts: []
};

it('renders correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(<AnalyzeTutorial component={component} currentUser={loggedInUser} {...props} />);
}
