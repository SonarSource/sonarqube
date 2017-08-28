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
import App from '../App';
import { Branch, BranchType } from '../../../../app/types';

it('renders sorted list of branches', () => {
  const branches: Branch[] = [
    { isMain: true, name: 'master' },
    { isMain: false, name: 'branch-1.0', type: BranchType.LONG },
    { isMain: false, name: 'branch-1.0', mergeBranch: 'master', type: BranchType.SHORT }
  ];
  expect(
    shallow(<App branches={branches} component={{ key: 'foo' }} onBranchesChange={jest.fn()} />)
  ).toMatchSnapshot();
});
