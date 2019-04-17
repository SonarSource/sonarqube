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
import ComponentSourceSnippetViewer from '../ComponentSourceSnippetViewer';
import {
  mockMainBranch,
  mockIssue,
  mockSourceViewerFile,
  mockFlowLocation,
  mockSnippetsByComponent
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/components', () => {
  const { mockSnippetsByComponent } = require.requireActual('../../../../helpers/testMocks');

  return {
    getSources: jest
      .fn()
      .mockResolvedValue(
        Object.values(
          mockSnippetsByComponent('a', [22, 23, 24, 25, 26, 27, 28, 29, 30, 31]).sources
        )
      )
  };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should expand block', async () => {
  const snippetGroup: T.SnippetGroup = {
    locations: [
      mockFlowLocation({
        component: 'a',
        textRange: { startLine: 34, endLine: 34, startOffset: 0, endOffset: 0 }
      }),
      mockFlowLocation({
        component: 'a',
        textRange: { startLine: 54, endLine: 54, startOffset: 0, endOffset: 0 }
      })
    ],
    ...mockSnippetsByComponent('a', [32, 33, 34, 35, 36, 52, 53, 54, 55, 56])
  };

  const wrapper = shallowRender({ snippetGroup });

  wrapper.instance().expandBlock(0, 'up');
  await waitAndUpdate(wrapper);

  expect(wrapper.state('snippets')).toHaveLength(2);
  expect(wrapper.state('snippets')[0]).toHaveLength(15);
  expect(Object.keys(wrapper.state('additionalLines'))).toHaveLength(10);
});

function shallowRender(props: Partial<ComponentSourceSnippetViewer['props']> = {}) {
  const snippetGroup: T.SnippetGroup = {
    component: mockSourceViewerFile(),
    locations: [],
    sources: []
  };
  return shallow<ComponentSourceSnippetViewer>(
    <ComponentSourceSnippetViewer
      branchLike={mockMainBranch()}
      highlightedLocationMessage={{ index: 0, text: '' }}
      issue={mockIssue()}
      issuesByLine={{}}
      last={false}
      locations={[]}
      onIssueChange={jest.fn()}
      onIssuePopupToggle={jest.fn()}
      onLocationSelect={jest.fn()}
      renderDuplicationPopup={jest.fn()}
      scroll={jest.fn()}
      snippetGroup={snippetGroup}
      {...props}
    />
  );
}
