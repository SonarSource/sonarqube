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
import IssueLabel from '../IssueLabel';
import { mockComponent, mockPullRequest, mockMeasure } from '../../../../helpers/testMocks';

it('should render correctly for bugs', () => {
  expect(
    shallowRender({
      measures: [mockMeasure({ metric: 'new_bugs' })]
    })
  ).toMatchSnapshot();
});

it('should render correctly for code smells', () => {
  expect(
    shallowRender({
      measures: [mockMeasure({ metric: 'new_code_smells' })],
      type: 'CODE_SMELL'
    })
  ).toMatchSnapshot();
});

it('should render correctly for vulnerabilities', () => {
  expect(
    shallowRender({
      measures: [mockMeasure({ metric: 'new_vulnerabilities' })],
      type: 'VULNERABILITY'
    })
  ).toMatchSnapshot();
});

it('should render correctly if no values are present', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(props = {}) {
  return shallow(
    <IssueLabel
      branchLike={mockPullRequest()}
      component={mockComponent()}
      measures={[]}
      type="BUG"
      {...props}
    />
  );
}
