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
import GlobalFooter from '../GlobalFooter';

it('should render the only logged in information', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should not render the only logged in information', () => {
  expect(
    getWrapper({ hideLoggedInInfo: true, sonarqubeVersion: '6.4-SNAPSHOT' })
  ).toMatchSnapshot();
});

it('should show the db warning message', () => {
  expect(getWrapper({ productionDatabase: false }).find('.alert')).toMatchSnapshot();
});

it('should display the sq version', () => {
  expect(getWrapper({ sonarqubeVersion: '6.4-SNAPSHOT' })).toMatchSnapshot();
});

it('should render SonarCloud footer', () => {
  expect(getWrapper({}, true)).toMatchSnapshot();
});

function getWrapper(props = {}, onSonarCloud = false) {
  return shallow(<GlobalFooter productionDatabase={true} {...props} />, {
    context: { onSonarCloud }
  });
}
