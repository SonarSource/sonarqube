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
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockIssue } from '../../../../helpers/testMocks';
import IssueTitleBar, { IssueTitleBarProps } from '../IssueTitleBar';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ onFilter: jest.fn() })).toMatchSnapshot('with filter');
  expect(shallowRender({ displayLocationsCount: true, issue: mockIssue(true) })).toMatchSnapshot(
    'with multi locations'
  );
  expect(
    shallowRender({
      branchLike: mockBranch(),
      displayLocationsCount: true,
      displayLocationsLink: true,
      issue: mockIssue(true),
    })
  ).toMatchSnapshot('with multi locations and link');
});

function shallowRender(props: Partial<IssueTitleBarProps> = {}) {
  return shallow<IssueTitleBarProps>(
    <IssueTitleBar
      issue={mockIssue(false, { externalRuleEngine: 'foo' })}
      togglePopup={jest.fn()}
      {...props}
    />
  );
}
