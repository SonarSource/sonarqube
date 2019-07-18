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
import { mockIssue, mockMainBranch } from '../../../../helpers/testMocks';
import IssuesSourceViewer from '../IssuesSourceViewer';

it('should render SourceViewer correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render CrossComponentSourceViewer correctly', () => {
  expect(
    shallowRender({ issues: [mockIssue(true)], openIssue: mockIssue(true) })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<IssuesSourceViewer['props']> = {}) {
  return shallow(
    <IssuesSourceViewer
      branchLike={mockMainBranch()}
      issues={[mockIssue()]}
      loadIssues={jest.fn()}
      locationsNavigator={true}
      onIssueChange={jest.fn()}
      onIssueSelect={jest.fn()}
      onLocationSelect={jest.fn()}
      openIssue={mockIssue()}
      selectedFlowIndex={undefined}
      selectedLocationIndex={undefined}
      {...props}
    />
  );
}
