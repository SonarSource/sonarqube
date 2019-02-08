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
import IssueTitleBar from '../IssueTitleBar';
import { mockIssue } from '../../../../helpers/testMocks';

const issue: T.Issue = mockIssue();
const issueWithLocations: T.Issue = mockIssue(true);

it('should render the titlebar correctly', () => {
  const branch: T.ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'feature-1.0',
    type: 'SHORT'
  };
  const element = shallow(
    <IssueTitleBar branchLike={branch} issue={issue} togglePopup={jest.fn()} />
  );
  expect(element).toMatchSnapshot();
});

it('should render the titlebar with the filter', () => {
  const element = shallow(
    <IssueTitleBar issue={issue} onFilter={jest.fn()} togglePopup={jest.fn()} />
  );
  expect(element).toMatchSnapshot();
});

it('should count all code locations', () => {
  const element = shallow(
    <IssueTitleBar
      displayLocationsCount={true}
      issue={issueWithLocations}
      togglePopup={jest.fn()}
    />
  );
  expect(element.find('LocationIndex')).toMatchSnapshot();
});

it('should have a correct permalink for security hotspots', () => {
  const wrapper = shallow(
    <IssueTitleBar issue={{ ...issue, type: 'SECURITY_HOTSPOT' }} togglePopup={jest.fn()} />
  );
  expect(wrapper.find('.js-issue-permalink').prop('to')).toEqual({
    pathname: '/project/issues',
    query: {
      id: 'myproject',
      issues: 'AVsae-CQS-9G3txfbFN2',
      open: 'AVsae-CQS-9G3txfbFN2',
      types: 'SECURITY_HOTSPOT'
    }
  });
});
