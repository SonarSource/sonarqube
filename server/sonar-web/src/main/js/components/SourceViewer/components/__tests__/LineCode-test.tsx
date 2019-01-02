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
import LineCode from '../LineCode';

const issueBase: T.Issue = {
  actions: [],
  component: '',
  componentLongName: '',
  componentQualifier: '',
  componentUuid: '',
  creationDate: '',
  key: '',
  flows: [],
  fromHotspot: false,
  message: '',
  organization: '',
  project: '',
  projectName: '',
  projectOrganization: '',
  projectKey: '',
  rule: '',
  ruleName: '',
  secondaryLocations: [],
  severity: '',
  status: '',
  transitions: [],
  type: 'BUG'
};

it('render code', () => {
  const line = {
    line: 3,
    code: '<span class="k">class</span> <span class="sym sym-1">Foo</span> {'
  };
  const issueLocations = [{ from: 0, to: 5, line: 3 }];
  const branch: T.ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'feature',
    type: 'SHORT'
  };
  const wrapper = shallow(
    <LineCode
      branchLike={branch}
      highlightedLocationMessage={undefined}
      highlightedSymbols={['sym1']}
      issueLocations={issueLocations}
      issuePopup={undefined}
      issues={[{ ...issueBase, key: 'issue-1' }, { ...issueBase, key: 'issue-2' }]}
      line={line}
      onIssueChange={jest.fn()}
      onIssuePopupToggle={jest.fn()}
      onIssueSelect={jest.fn()}
      onLocationSelect={jest.fn()}
      onSymbolClick={jest.fn()}
      secondaryIssueLocations={[]}
      selectedIssue="issue-1"
      showIssues={true}
    />
  );
  expect(wrapper).toMatchSnapshot();
});
