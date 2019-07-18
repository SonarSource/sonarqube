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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockComponent, mockMeasure, mockPullRequest } from '../../../../helpers/testMocks';
import IssueRating from '../IssueRating';

it('should render correctly for bugs', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly for code smells', () => {
  expect(shallowRender({ type: 'CODE_SMELL' })).toMatchSnapshot();
});

it('should render correctly for vulnerabilities', () => {
  expect(shallowRender({ type: 'VULNERABILITY' })).toMatchSnapshot();
});

it('should render correctly if no values are present', () => {
  expect(shallowRender({ measures: [mockMeasure({ metric: 'NONE' })] })).toMatchSnapshot();
});

function shallowRender(props = {}) {
  return shallow(
    <IssueRating
      branchLike={mockPullRequest()}
      component={mockComponent()}
      measures={[
        mockMeasure({ metric: 'new_reliability_rating' }),
        mockMeasure({ metric: 'new_maintainability_rating' }),
        mockMeasure({ metric: 'new_security_rating' })
      ]}
      type="BUG"
      {...props}
    />
  );
}
