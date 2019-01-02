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
import TaskComponent from '../TaskComponent';

const TASK = {
  componentKey: 'foo',
  componentName: 'foo',
  componentQualifier: 'TRK',
  id: 'bar',
  organization: 'org',
  status: 'PENDING',
  submittedAt: '2017-01-01',
  submitterLogin: 'yoda',
  type: 'REPORT'
};

it('renders correctly', () => {
  expect(shallow(<TaskComponent task={TASK} />)).toMatchSnapshot();
  expect(shallow(<TaskComponent task={{ ...TASK, componentKey: undefined }} />)).toMatchSnapshot();
});

it('renders correctly for branches and pullrequest', () => {
  expect(
    shallow(<TaskComponent task={{ ...TASK, branch: 'feature', branchType: 'SHORT' }} />)
  ).toMatchSnapshot();
  expect(
    shallow(<TaskComponent task={{ ...TASK, branch: 'branch-6.7', branchType: 'LONG' }} />)
  ).toMatchSnapshot();
  expect(shallow(<TaskComponent task={{ ...TASK, pullRequest: 'pr-89' }} />)).toMatchSnapshot();
});
