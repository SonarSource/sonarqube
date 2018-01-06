/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import IssueTitleBar from '../IssueTitleBar';

const issue = {
  line: 25,
  textRange: {
    startLine: 25,
    endLine: 26,
    startOffset: 0,
    endOffset: 15
  },
  creationDate: '2017-03-01T09:36:01+0100',
  organization: 'myorg',
  project: 'myproject',
  key: 'AVsae-CQS-9G3txfbFN2',
  rule: 'javascript:S1067',
  message: 'Reduce the number of conditional operators (4) used in the expression',
  flows: [],
  secondaryLocations: []
};

const issueWithLocations = {
  ...issue,
  flows: [[{}, {}, {}], [{}, {}]],
  secondaryLocations: [{}, {}]
};

it('should render the titlebar correctly', () => {
  const element = shallow(
    <IssueTitleBar
      branch="feature-1.0"
      issue={issue}
      currentPopup={null}
      onFail={jest.fn()}
      togglePopup={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should render the titlebar with the filter', () => {
  const element = shallow(
    <IssueTitleBar
      issue={issue}
      currentPopup={null}
      onFail={jest.fn()}
      onFilter={jest.fn()}
      togglePopup={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should count all code locations', () => {
  const element = shallow(
    <IssueTitleBar displayLocationsCount={true} issue={issueWithLocations} />
  );
  expect(element.find('LocationIndex')).toMatchSnapshot();
});
