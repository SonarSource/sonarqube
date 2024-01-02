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
import { range, times } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { getSources } from '../../../../api/components';
import IssueMessageBox from '../../../../components/issue/IssueMessageBox';
import { mockBranch, mockMainBranch } from '../../../../helpers/mocks/branch-like';
import {
  mockSnippetsByComponent,
  mockSourceLine,
  mockSourceViewerFile,
} from '../../../../helpers/mocks/sources';
import { mockFlowLocation, mockIssue } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import { IssueStatus } from '../../../../types/issues';
import { SnippetGroup } from '../../../../types/types';
import ComponentSourceSnippetGroupViewer from '../ComponentSourceSnippetGroupViewer';
import SnippetViewer from '../SnippetViewer';

jest.mock('../../../../api/components', () => ({
  getSources: jest.fn().mockResolvedValue([]),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly with secondary locations', () => {
  // issue with secondary locations but no flows
  const issue = mockIssue(true, {
    component: 'project:main.js',
    flows: [],
    textRange: { startLine: 7, endLine: 7, startOffset: 5, endOffset: 10 },
  });

  const snippetGroup: SnippetGroup = {
    locations: [
      mockFlowLocation({
        component: issue.component,
        textRange: { startLine: 34, endLine: 34, startOffset: 0, endOffset: 0 },
      }),
      mockFlowLocation({
        component: issue.component,
        textRange: { startLine: 74, endLine: 74, startOffset: 0, endOffset: 0 },
      }),
    ],
    ...mockSnippetsByComponent('main.js', 'project', [
      ...range(2, 17),
      ...range(29, 39),
      ...range(69, 79),
    ]),
  };
  const wrapper = shallowRender({ issue, snippetGroup });
  expect(wrapper.state('snippets')).toHaveLength(3);
  expect(wrapper.state('snippets')[0]).toEqual({ index: 0, start: 2, end: 16 });
  expect(wrapper.state('snippets')[1]).toEqual({ index: 2, start: 29, end: 39 });
  expect(wrapper.state('snippets')[2]).toEqual({ index: 3, start: 69, end: 79 });
});

it('should render correctly with flows', () => {
  // issue with flows but no secondary locations
  const issue = mockIssue(true, {
    component: 'project:main.js',
    secondaryLocations: [],
    textRange: { startLine: 7, endLine: 7, startOffset: 5, endOffset: 10 },
  });

  const snippetGroup: SnippetGroup = {
    locations: [
      mockFlowLocation({
        component: issue.component,
        textRange: { startLine: 34, endLine: 34, startOffset: 0, endOffset: 0 },
      }),
      mockFlowLocation({
        component: issue.component,
        textRange: { startLine: 74, endLine: 74, startOffset: 0, endOffset: 0 },
      }),
    ],
    ...mockSnippetsByComponent('main.js', 'project', [
      ...range(2, 17),
      ...range(29, 39),
      ...range(69, 79),
    ]),
  };
  const wrapper = shallowRender({ issue, snippetGroup });
  expect(wrapper.state('snippets')).toHaveLength(3);
  expect(wrapper.state('snippets')[0]).toEqual({ index: 0, start: 2, end: 16 });
  expect(wrapper.state('snippets')[1]).toEqual({ index: 1, start: 29, end: 39 });
  expect(wrapper.state('snippets')[2]).toEqual({ index: 2, start: 69, end: 79 });

  // Check that locationsByLine is defined when isLastOccurenceOfPrimaryComponent
  expect(wrapper.find(SnippetViewer).at(0).props().locationsByLine).not.toEqual({});

  // If not, it should be an empty object:
  const snippets = shallowRender({
    isLastOccurenceOfPrimaryComponent: false,
    issue,
    snippetGroup,
  }).find(SnippetViewer);

  expect(snippets.at(0).props().locationsByLine).toEqual({});
  expect(snippets.at(1).props().locationsByLine).toEqual({});
});

it('should render file-level issue correctly', () => {
  // issue with secondary locations and no primary location
  const issue = mockIssue(true, {
    component: 'project:main.js',
    flows: [],
    textRange: undefined,
  });

  const wrapper = shallowRender({
    issue,
    snippetGroup: {
      locations: [
        mockFlowLocation({
          component: issue.component,
          textRange: { startLine: 34, endLine: 34, startOffset: 0, endOffset: 0 },
        }),
      ],
      ...mockSnippetsByComponent('main.js', 'project', range(29, 39)),
    },
  });

  expect(wrapper.find('ContextConsumer').dive().find(IssueMessageBox).exists()).toBe(true);
});

it.each([
  ['file-level', ComponentQualifier.File, true, 'issue.closed.file_level'],
  ['file-level', ComponentQualifier.File, false, 'issue.closed.project_level'],
  ['project-level', ComponentQualifier.Project, false, 'issue.closed.project_level'],
])(
  'should render a closed %s issue correctly',
  async (_level, componentQualifier, componentEnabled, expectedLabel) => {
    // issue with secondary locations and no primary location
    const issue = mockIssue(true, {
      component: 'project:main.js',
      componentQualifier,
      componentEnabled,
      flows: [],
      textRange: undefined,
      status: IssueStatus.Closed,
    });

    const wrapper = shallowRender({
      issue,
      snippetGroup: {
        locations: [],
        ...mockSnippetsByComponent('main.js', 'project', range(1, 10)),
      },
    });

    await waitAndUpdate(wrapper);

    expect(wrapper.find<FormattedMessage>(FormattedMessage).prop('id')).toEqual(expectedLabel);
    expect(wrapper.find('ContextConsumer').exists()).toBe(false);
  }
);

it('should expand block', async () => {
  (getSources as jest.Mock).mockResolvedValueOnce(
    Object.values(mockSnippetsByComponent('a', 'project', range(6, 59)).sources)
  );
  const issue = mockIssue(true, {
    textRange: { startLine: 74, endLine: 74, startOffset: 5, endOffset: 10 },
  });
  const snippetGroup: SnippetGroup = {
    locations: [
      mockFlowLocation({
        component: 'a',
        textRange: { startLine: 74, endLine: 74, startOffset: 0, endOffset: 0 },
      }),
      mockFlowLocation({
        component: 'a',
        textRange: { startLine: 107, endLine: 107, startOffset: 0, endOffset: 0 },
      }),
    ],
    ...mockSnippetsByComponent('a', 'project', [...range(69, 83), ...range(102, 112)]),
  };

  const wrapper = shallowRender({ issue, snippetGroup });

  wrapper.instance().expandBlock(0, 'up');
  await waitAndUpdate(wrapper);

  expect(getSources).toHaveBeenCalledWith({ from: 9, key: 'project:a', to: 68 });
  expect(wrapper.state('snippets')).toHaveLength(2);
  expect(wrapper.state('snippets')[0]).toEqual({ index: 0, start: 19, end: 83 });
  expect(Object.keys(wrapper.state('additionalLines'))).toHaveLength(53);
});

it('should expand full component', async () => {
  (getSources as jest.Mock).mockResolvedValueOnce(
    Object.values(mockSnippetsByComponent('a', 'project', times(14)).sources)
  );
  const snippetGroup: SnippetGroup = {
    locations: [
      mockFlowLocation({
        component: 'a',
        textRange: { startLine: 3, endLine: 3, startOffset: 0, endOffset: 0 },
      }),
      mockFlowLocation({
        component: 'a',
        textRange: { startLine: 12, endLine: 12, startOffset: 0, endOffset: 0 },
      }),
    ],
    ...mockSnippetsByComponent('a', 'project', [1, 2, 3, 4, 5, 10, 11, 12, 13, 14]),
  };

  const wrapper = shallowRender({ snippetGroup });

  wrapper.instance().expandComponent();
  await waitAndUpdate(wrapper);

  expect(getSources).toHaveBeenCalledWith({ key: 'project:a' });
  expect(wrapper.state('snippets')).toHaveLength(1);
  expect(wrapper.state('snippets')[0]).toEqual({ index: -1, start: 0, end: 13 });
});

it('should get the right branch when expanding', async () => {
  (getSources as jest.Mock).mockResolvedValueOnce(
    Object.values(mockSnippetsByComponent('a', 'project', range(8, 67)).sources)
  );
  const snippetGroup: SnippetGroup = {
    locations: [mockFlowLocation()],
    ...mockSnippetsByComponent('a', 'project', [1, 2, 3, 4, 5, 6, 7]),
  };

  const wrapper = shallowRender({
    branchLike: mockBranch({ name: 'asdf' }),
    snippetGroup,
  });

  wrapper.instance().expandBlock(0, 'down');
  await waitAndUpdate(wrapper);

  expect(getSources).toHaveBeenCalledWith({ branch: 'asdf', from: 8, key: 'project:a', to: 67 });
});

it('should handle symbol highlighting', () => {
  const wrapper = shallowRender();
  expect(wrapper.state('highlightedSymbols')).toEqual([]);
  wrapper.instance().handleSymbolClick(['foo']);
  expect(wrapper.state('highlightedSymbols')).toEqual(['foo']);
  wrapper.instance().handleSymbolClick(['foo']);
  expect(wrapper.state('highlightedSymbols')).toEqual([]);
});

it('should correctly handle lines actions', () => {
  const snippetGroup: SnippetGroup = {
    locations: [
      mockFlowLocation({
        component: 'my-project:foo/bar.ts',
        textRange: { startLine: 34, endLine: 34, startOffset: 0, endOffset: 0 },
      }),
      mockFlowLocation({
        component: 'my-project:foo/bar.ts',
        textRange: { startLine: 54, endLine: 54, startOffset: 0, endOffset: 0 },
      }),
    ],
    ...mockSnippetsByComponent(
      'foo/bar.ts',
      'my-project',
      [32, 33, 34, 35, 36, 52, 53, 54, 55, 56]
    ),
  };
  const loadDuplications = jest.fn();
  const renderDuplicationPopup = jest.fn();

  const wrapper = shallowRender({
    loadDuplications,
    renderDuplicationPopup,
    snippetGroup,
  });

  const line = mockSourceLine();
  wrapper.find('SnippetViewer').first().prop<Function>('loadDuplications')(line);
  expect(loadDuplications).toHaveBeenCalledWith('my-project:foo/bar.ts', line);

  wrapper.find('SnippetViewer').first().prop<Function>('renderDuplicationPopup')(1, 13);
  expect(renderDuplicationPopup).toHaveBeenCalledWith(
    mockSourceViewerFile('foo/bar.ts', 'my-project'),
    1,
    13
  );
});

function shallowRender(props: Partial<ComponentSourceSnippetGroupViewer['props']> = {}) {
  const snippetGroup: SnippetGroup = {
    component: mockSourceViewerFile(),
    locations: [],
    sources: [],
  };
  return shallow<ComponentSourceSnippetGroupViewer>(
    <ComponentSourceSnippetGroupViewer
      branchLike={mockMainBranch()}
      highlightedLocationMessage={{ index: 0, text: '' }}
      isLastOccurenceOfPrimaryComponent={true}
      issue={mockIssue()}
      issuesByLine={{}}
      lastSnippetGroup={false}
      loadDuplications={jest.fn()}
      locations={[]}
      onIssueSelect={jest.fn()}
      onLocationSelect={jest.fn()}
      renderDuplicationPopup={jest.fn()}
      snippetGroup={snippetGroup}
      {...props}
    />
  );
}
