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
import * as React from 'react';
import { shallow } from 'enzyme';
import ConciseIssueLocations from '../ConciseIssueLocations';

const textRange = { startLine: 1, startOffset: 1, endLine: 1, endOffset: 1 };

const baseIssue = {
  component: '',
  componentLongName: '',
  componentQualifier: '',
  componentUuid: '',
  creationDate: '',
  key: '',
  message: '',
  organization: '',
  project: '',
  projectName: '',
  projectOrganization: '',
  projectUuid: '',
  rule: '',
  ruleName: '',
  severity: '',
  status: '',
  type: '',
  secondaryLocations: [],
  flows: []
};

it('should render secondary locations', () => {
  const issue = {
    ...baseIssue,
    secondaryLocations: [{ msg: '', textRange }, { msg: '', textRange }, { msg: '', textRange }]
  };
  expect(
    shallow(
      <ConciseIssueLocations issue={issue} onFlowSelect={jest.fn()} selectedFlowIndex={undefined} />
    )
  ).toMatchSnapshot();
});

it('should render one flow', () => {
  const issue = {
    ...baseIssue,
    secondaryLocations: [],
    flows: [[{ msg: '', textRange }, { msg: '', textRange }, { msg: '', textRange }]]
  };
  expect(
    shallow(
      <ConciseIssueLocations issue={issue} onFlowSelect={jest.fn()} selectedFlowIndex={undefined} />
    )
  ).toMatchSnapshot();
});

it('should render several flows', () => {
  const issue = {
    ...baseIssue,
    flows: [
      [{ msg: '', textRange }, { msg: '', textRange }, { msg: '', textRange }],
      [{ msg: '', textRange }, { msg: '', textRange }],
      [{ msg: '', textRange }, { msg: '', textRange }, { msg: '', textRange }]
    ]
  };
  expect(
    shallow(
      <ConciseIssueLocations issue={issue} onFlowSelect={jest.fn()} selectedFlowIndex={undefined} />
    )
  ).toMatchSnapshot();
});
