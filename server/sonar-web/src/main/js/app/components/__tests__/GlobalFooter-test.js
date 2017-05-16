/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import GlobalFooter from '../GlobalFooter';

it('should render the only logged in information', () => {
  expect(
    shallow(<GlobalFooter productionDatabase={true} sonarqubeDotCom={false} />)
  ).toMatchSnapshot();
});

it('should not render the only logged in information', () => {
  expect(
    shallow(
      <GlobalFooter
        hideLoggedInInfo={true}
        productionDatabase={true}
        sonarqubeDotCom={false}
        sonarqubeVersion="6.4-SNAPSHOT"
      />
    )
  ).toMatchSnapshot();
});

it('should show the db warning message', () => {
  expect(
    shallow(<GlobalFooter productionDatabase={false} sonarqubeDotCom={false} />).find('.alert')
  ).toMatchSnapshot();
});

it('should display the sq version', () => {
  expect(
    shallow(
      <GlobalFooter
        productionDatabase={true}
        sonarqubeDotCom={false}
        sonarqubeVersion="6.4-SNAPSHOT"
      />
    )
  ).toMatchSnapshot();
});

it('should render SonarqubeDotCom footer', () => {
  expect(
    shallow(<GlobalFooter productionDatabase={true} sonarqubeDotCom={true} />)
  ).toMatchSnapshot();
});
