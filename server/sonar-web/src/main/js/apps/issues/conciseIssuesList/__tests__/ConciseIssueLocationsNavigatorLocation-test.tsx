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
import ConciseIssueLocationsNavigatorLocation from '../ConciseIssueLocationsNavigatorLocation';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});
it('should render vulnerabilities correctly', () => {
  expect(shallowRender({ index: 0, isTaintAnalysis: true, totalCount: 4 })).toMatchSnapshot();
  expect(shallowRender({ index: 1, isTaintAnalysis: true, totalCount: 4 })).toMatchSnapshot();
  expect(shallowRender({ index: 3, isTaintAnalysis: true, totalCount: 4 })).toMatchSnapshot();
});

const shallowRender = (props: Partial<ConciseIssueLocationsNavigatorLocation['props']> = {}) => {
  return shallow(
    <ConciseIssueLocationsNavigatorLocation
      index={0}
      isTaintAnalysis={false}
      message=""
      onClick={jest.fn()}
      scroll={jest.fn()}
      selected={true}
      totalCount={5}
      {...props}
    />
  );
};
