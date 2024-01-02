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
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockSourceViewerFile } from '../../../../helpers/mocks/sources';
import { ComponentQualifier } from '../../../../types/component';
import IssueSourceViewerHeader, { Props } from '../IssueSourceViewerHeader';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ linkToProject: false })).toMatchSnapshot('no link to project');
  expect(shallowRender({ displayProjectName: false })).toMatchSnapshot('no project name');
  expect(
    shallowRender({
      sourceViewerFile: mockSourceViewerFile('foo/bar.ts', 'my-project', {
        q: ComponentQualifier.Project,
      }),
    })
  ).toMatchSnapshot('project root');
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <IssueSourceViewerHeader
      branchLike={mockMainBranch()}
      expandable={true}
      onExpand={jest.fn()}
      sourceViewerFile={mockSourceViewerFile('foo/bar.ts', 'my-project')}
      {...props}
    />
  );
}
