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
import { mockIssue, mockMainBranch, mockSourceViewerFile } from '../../../helpers/testMocks';
import SourceViewerHeader from '../SourceViewerHeader';

it('should render correctly for a regular file', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly for a unit test', () => {
  expect(
    shallowRender({
      showMeasures: true,
      sourceViewerFile: mockSourceViewerFile({ q: 'UTS', measures: { tests: '12' } })
    })
  ).toMatchSnapshot();
});

it('should render correctly if issue details are passed', () => {
  const issues = [
    mockIssue(false, { type: 'VULNERABILITY' }),
    mockIssue(false, { type: 'VULNERABILITY' }),
    mockIssue(false, { type: 'CODE_SMELL' }),
    mockIssue(false, { type: 'SECURITY_HOTSPOT' }),
    mockIssue(false, { type: 'SECURITY_HOTSPOT' })
  ];

  expect(
    shallowRender({
      issues,
      showMeasures: true
    })
  ).toMatchSnapshot();

  expect(
    shallowRender({
      issues,
      showMeasures: false
    })
      .find('.source-viewer-header-measure')
      .exists()
  ).toBe(false);
});

function shallowRender(props: Partial<SourceViewerHeader['props']> = {}) {
  return shallow(
    <SourceViewerHeader
      branchLike={mockMainBranch()}
      openComponent={jest.fn()}
      sourceViewerFile={mockSourceViewerFile()}
      {...props}
    />
  );
}
