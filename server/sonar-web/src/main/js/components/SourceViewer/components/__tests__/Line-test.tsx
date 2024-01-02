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
import { mockSourceLine } from '../../../../helpers/mocks/sources';
import { mockIssue } from '../../../../helpers/testMocks';
import Line from '../Line';

it('should render correctly for last, new, and highlighted lines', () => {
  expect(
    shallowRender({
      highlighted: true,
      last: true,
      line: mockSourceLine({ isNew: true }),
    })
  ).toMatchSnapshot();
});

it('handles the opening and closing of issues', () => {
  const line = mockSourceLine();
  const issue = mockIssue();
  const onIssuesClose = jest.fn();
  const onIssueUnselect = jest.fn();
  const onIssuesOpen = jest.fn();
  const onIssueSelect = jest.fn();
  const wrapper = shallowRender({
    issues: [issue],
    line,
    onIssuesClose,
    onIssueSelect,
    onIssuesOpen,
    onIssueUnselect,
    openIssues: true,
  });
  const instance = wrapper.instance();

  instance.handleIssuesIndicatorClick();
  expect(onIssuesClose).toHaveBeenCalledWith(line);
  expect(onIssueUnselect).toHaveBeenCalled();

  wrapper.setProps({ openIssues: false });
  instance.handleIssuesIndicatorClick();
  expect(onIssuesOpen).toHaveBeenCalledWith(line);
  expect(onIssueSelect).toHaveBeenCalledWith(issue.key);
});

function shallowRender(props: Partial<Line['props']> = {}) {
  return shallow<Line>(
    <Line
      displayAllIssues={false}
      displayCoverage={false}
      displayDuplications={false}
      displayIssues={false}
      displayLocationMarkers={false}
      duplications={[0]}
      duplicationsCount={0}
      firstLineNumber={1}
      highlighted={false}
      highlightedLocationMessage={undefined}
      highlightedSymbols={undefined}
      issueLocations={[]}
      issues={[mockIssue(), mockIssue(false, { type: 'VULNERABILITY' })]}
      last={false}
      line={mockSourceLine()}
      loadDuplications={jest.fn()}
      onIssuesClose={jest.fn()}
      onIssueSelect={jest.fn()}
      onIssuesOpen={jest.fn()}
      onIssueUnselect={jest.fn()}
      onLocationSelect={jest.fn()}
      onSymbolClick={jest.fn()}
      openIssues={false}
      previousLine={undefined}
      renderDuplicationPopup={jest.fn()}
      secondaryIssueLocations={[]}
      {...props}
    />
  );
}
