/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
// import { click } from '../../../../helpers/testUtils';
import LineCode from '../LineCode';

it('render code', () => {
  const line = {
    line: 3,
    code: '<span class="k">class</span> <span class="sym sym-1">Foo</span> {'
  };
  const issueLocations = [{ from: 0, to: 5, line: 3 }];
  const secondaryIssueLocations = [{ from: 6, to: 9, line: 3 }];
  const secondaryIssueLocationMessages = [{ msg: 'Fix that', flowIndex: 0, locationIndex: 0 }];
  const selectedIssueLocation = { from: 6, to: 9, line: 3, flowIndex: 0, locationIndex: 0 };
  const wrapper = shallow(
    <LineCode
      highlightedSymbol="sym1"
      issueKeys={['issue-1', 'issue-2']}
      issueLocations={issueLocations}
      line={line}
      onIssueSelect={jest.fn()}
      onSelectLocation={jest.fn()}
      onSymbolClick={jest.fn()}
      secondaryIssueLocations={secondaryIssueLocations}
      secondaryIssueLocationMessages={secondaryIssueLocationMessages}
      selectedIssue="issue-1"
      selectedIssueLocation={selectedIssueLocation}
      showIssues={true}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should handle empty location message', () => {
  const line = {
    line: 3,
    code: '<span class="k">class</span>'
  };
  const issueLocations = [{ from: 0, to: 5, line: 3 }];
  const secondaryIssueLocations = [{ from: 6, to: 9, line: 3 }];
  const secondaryIssueLocationMessages = [{ flowIndex: 0, locationIndex: 0 }];
  const selectedIssueLocation = { from: 6, to: 9, line: 3, flowIndex: 0, locationIndex: 0 };
  const wrapper = shallow(
    <LineCode
      highlightedSymbol="sym1"
      issueKeys={['issue-1', 'issue-2']}
      issueLocations={issueLocations}
      line={line}
      onIssueSelect={jest.fn()}
      onSelectLocation={jest.fn()}
      onSymbolClick={jest.fn()}
      secondaryIssueLocations={secondaryIssueLocations}
      secondaryIssueLocationMessages={secondaryIssueLocationMessages}
      selectedIssue="issue-1"
      selectedIssueLocation={selectedIssueLocation}
      showIssues={true}
    />
  );
  expect(wrapper.find('.source-line-issue-locations')).toMatchSnapshot();
});
