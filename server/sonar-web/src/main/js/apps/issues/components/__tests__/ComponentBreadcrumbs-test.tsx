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
import ComponentBreadcrumbs from '../ComponentBreadcrumbs';
import { ShortLivingBranch, BranchType } from '../../../../app/types';

const baseIssue = {
  component: 'comp',
  componentLongName: 'comp-name',
  organization: 'org',
  project: 'proj',
  projectName: 'proj-name'
};

it('renders', () => {
  expect(shallow(<ComponentBreadcrumbs issue={baseIssue} />)).toMatchSnapshot();
});

it('renders with sub-project', () => {
  const issue = { ...baseIssue, subProject: 'sub-proj', subProjectName: 'sub-proj-name' };
  expect(shallow(<ComponentBreadcrumbs issue={issue} />)).toMatchSnapshot();
});

it('renders with branch', () => {
  const issue = { ...baseIssue, subProject: 'sub-proj', subProjectName: 'sub-proj-name' };
  const shortBranch: ShortLivingBranch = {
    isMain: false,
    mergeBranch: '',
    name: 'feature',
    type: BranchType.SHORT
  };
  expect(
    shallow(<ComponentBreadcrumbs branchLike={shortBranch} issue={issue} />)
  ).toMatchSnapshot();
});
