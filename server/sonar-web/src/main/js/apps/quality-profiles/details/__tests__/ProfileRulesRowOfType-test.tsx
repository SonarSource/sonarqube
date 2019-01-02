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
import ProfileRulesRowOfType from '../ProfileRulesRowOfType';

it('should render correctly', () => {
  expect(
    shallow(
      <ProfileRulesRowOfType count={3} organization="foo" qprofile="bar" total={10} type="BUG" />
    )
  ).toMatchSnapshot();
});

it('should render correctly if there is 0 rules', () => {
  expect(
    shallow(
      <ProfileRulesRowOfType
        count={0}
        organization={null}
        qprofile="bar"
        total={0}
        type="VULNERABILITY"
      />
    )
  ).toMatchSnapshot();
});

it('should render correctly if there is missing data', () => {
  expect(
    shallow(
      <ProfileRulesRowOfType
        count={5}
        organization={null}
        qprofile="bar"
        total={null}
        type="VULNERABILITY"
      />
    )
  ).toMatchSnapshot();
  expect(
    shallow(
      <ProfileRulesRowOfType
        count={null}
        organization={null}
        qprofile="foo"
        total={10}
        type="VULNERABILITY"
      />
    )
  ).toMatchSnapshot();
});
