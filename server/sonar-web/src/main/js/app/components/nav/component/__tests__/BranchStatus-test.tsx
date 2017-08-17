/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import BranchStatus from '../BranchStatus';
import { BranchType } from '../../../../types';

it('renders', () => {
  check(0, 0, 0);
  check(0, 1, 0);
  check(7, 3, 6);
});

function check(bugs: number, codeSmells: number, vulnerabilities: number) {
  expect(
    shallow(
      <BranchStatus
        branch={{
          isMain: false,
          name: 'foo',
          status: { bugs, codeSmells, vulnerabilities },
          type: BranchType.SHORT
        }}
      />
    )
  ).toMatchSnapshot();
}
