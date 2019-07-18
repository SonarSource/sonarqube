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
import { mockIssue, mockPullRequest, mockSourceLine } from '../../../../helpers/testMocks';
import Line from '../Line';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly for last, new, and highlighted lines', () => {
  expect(
    shallowRender({
      highlighted: true,
      last: true,
      line: mockSourceLine({ isNew: true })
    })
  ).toMatchSnapshot();
});

it('should render correctly with coverage', () => {
  expect(
    shallowRender({
      displayCoverage: true
    })
  ).toMatchSnapshot();
});

it('should render correctly with duplication information', () => {
  expect(
    shallowRender({
      displayDuplications: true,
      duplicationsCount: 3
    })
  ).toMatchSnapshot();
});

it('should render correctly with issues info', () => {
  expect(shallowRender({ displayIssues: true })).toMatchSnapshot();
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
    openIssues: true
  });
  const instance = wrapper.instance();

  instance.handleIssuesIndicatorClick();
  expect(onIssuesClose).toBeCalledWith(line);
  expect(onIssueUnselect).toBeCalled();

  wrapper.setProps({ openIssues: false });
  instance.handleIssuesIndicatorClick();
  expect(onIssuesOpen).toBeCalledWith(line);
  expect(onIssueSelect).toBeCalledWith(issue.key);
});

function shallowRender(props: Partial<Line['props']> = {}) {
  return shallow<Line>(
    <Line
      branchLike={mockPullRequest()}
      displayAllIssues={false}
      displayCoverage={false}
      displayDuplications={false}
      displayIssues={false}
      displayLocationMarkers={false}
      duplications={[]}
      duplicationsCount={0}
      highlighted={false}
      highlightedLocationMessage={undefined}
      highlightedSymbols={undefined}
      issueLocations={[]}
      issuePopup={undefined}
      issues={[mockIssue(), mockIssue(false, { type: 'VULNERABILITY' })]}
      last={false}
      line={mockSourceLine()}
      linePopup={undefined}
      loadDuplications={jest.fn()}
      onLinePopupToggle={jest.fn()}
      onIssueChange={jest.fn()}
      onIssuePopupToggle={jest.fn()}
      onIssuesClose={jest.fn()}
      onIssueSelect={jest.fn()}
      onIssuesOpen={jest.fn()}
      onIssueUnselect={jest.fn()}
      onLocationSelect={jest.fn()}
      onSymbolClick={jest.fn()}
      openIssues={false}
      previousLine={undefined}
      renderDuplicationPopup={jest.fn()}
      scroll={jest.fn()}
      secondaryIssueLocations={[]}
      selectedIssue={undefined}
      {...props}
    />
  );
}
