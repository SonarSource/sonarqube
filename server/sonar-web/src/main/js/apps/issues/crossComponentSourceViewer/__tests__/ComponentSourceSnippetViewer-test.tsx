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
import { mount, ReactWrapper, shallow } from 'enzyme';
import { range, times } from 'lodash';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getSources } from '../../../../api/components';
import {
  mockFlowLocation,
  mockIssue,
  mockMainBranch,
  mockShortLivingBranch,
  mockSnippetsByComponent,
  mockSourceLine,
  mockSourceViewerFile
} from '../../../../helpers/testMocks';
import ComponentSourceSnippetViewer from '../ComponentSourceSnippetViewer';

jest.mock('../../../../api/components', () => ({
  getSources: jest.fn().mockResolvedValue([])
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
    flows: [],
    textRange: { startLine: 5, endLine: 5, startOffset: 5, endOffset: 10 }
  });

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
    ...mockSnippetsByComponent('a', [...range(3, 15), 32, 33, 34, 35, 36, 52, 53, 54, 55, 56])
  };
  const wrapper = shallowRender({ issue, snippetGroup });
  expect(wrapper.state('snippets')).toHaveLength(3);
  expect(wrapper.state('snippets')[0]).toEqual({ index: 0, start: 3, end: 14 });
  expect(wrapper.state('snippets')[1]).toEqual({ index: 1, start: 32, end: 36 });
  expect(wrapper.state('snippets')[2]).toEqual({ index: 2, start: 52, end: 56 });
});

it('should expand block', async () => {
  (getSources as jest.Mock).mockResolvedValueOnce(
    Object.values(mockSnippetsByComponent('a', [22, 23, 24, 25, 26, 27, 28, 29, 30, 31]).sources)
  );
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

  expect(getSources).toHaveBeenCalledWith({ from: 19, key: 'a', to: 31 });
  expect(wrapper.state('snippets')).toHaveLength(2);
  expect(wrapper.state('snippets')[0]).toEqual({ index: 0, start: 22, end: 36 });
  expect(Object.keys(wrapper.state('additionalLines'))).toHaveLength(10);
});

it('should expand full component', async () => {
  (getSources as jest.Mock).mockResolvedValueOnce(
    Object.values(mockSnippetsByComponent('a', times(14)).sources)
  );
  const snippetGroup: T.SnippetGroup = {
    locations: [
      mockFlowLocation({
        component: 'a',
        textRange: { startLine: 3, endLine: 3, startOffset: 0, endOffset: 0 }
      }),
      mockFlowLocation({
        component: 'a',
        textRange: { startLine: 12, endLine: 12, startOffset: 0, endOffset: 0 }
      })
    ],
    ...mockSnippetsByComponent('a', [1, 2, 3, 4, 5, 10, 11, 12, 13, 14])
  };

  const wrapper = shallowRender({ snippetGroup });

  wrapper.instance().expandComponent();
  await waitAndUpdate(wrapper);

  expect(getSources).toHaveBeenCalledWith({ key: 'a' });
  expect(wrapper.state('snippets')).toHaveLength(1);
  expect(wrapper.state('snippets')[0]).toEqual({ index: -1, start: 0, end: 13 });
});

it('should get the right branch when expanding', async () => {
  (getSources as jest.Mock).mockResolvedValueOnce(
    Object.values(
      mockSnippetsByComponent('a', [5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17]).sources
    )
  );
  const snippetGroup: T.SnippetGroup = {
    locations: [mockFlowLocation()],
    ...mockSnippetsByComponent('a', [1, 2, 3, 4])
  };

  const wrapper = shallowRender({
    branchLike: mockShortLivingBranch({ name: 'asdf' }),
    snippetGroup
  });

  wrapper.instance().expandBlock(0, 'down');
  await waitAndUpdate(wrapper);

  expect(getSources).toHaveBeenCalledWith({ branch: 'asdf', from: 5, key: 'a', to: 17 });
});

it('should handle correctly open/close issue', () => {
  const wrapper = shallowRender();
  const sourceLine = mockSourceLine();
  expect(wrapper.state('openIssuesByLine')).toEqual({});
  wrapper.instance().handleOpenIssues(sourceLine);
  expect(wrapper.state('openIssuesByLine')).toEqual({ [sourceLine.line]: true });
  wrapper.instance().handleCloseIssues(sourceLine);
  expect(wrapper.state('openIssuesByLine')).toEqual({ [sourceLine.line]: false });
});

it('should handle symbol highlighting', () => {
  const wrapper = shallowRender();
  expect(wrapper.state('highlightedSymbols')).toEqual([]);
  wrapper.instance().handleSymbolClick(['foo']);
  expect(wrapper.state('highlightedSymbols')).toEqual(['foo']);
});

it('should correctly handle lines actions', () => {
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
  const loadDuplications = jest.fn();
  const onLinePopupToggle = jest.fn();
  const renderDuplicationPopup = jest.fn();

  const wrapper = shallowRender({
    loadDuplications,
    onLinePopupToggle,
    renderDuplicationPopup,
    snippetGroup
  });

  const line = mockSourceLine();
  wrapper
    .find('SnippetViewer')
    .first()
    .prop<Function>('loadDuplications')(line);
  expect(loadDuplications).toHaveBeenCalledWith('a', line);

  wrapper
    .find('SnippetViewer')
    .first()
    .prop<Function>('handleLinePopupToggle')({ line: 13, name: 'foo' });
  expect(onLinePopupToggle).toHaveBeenCalledWith({ component: 'a', line: 13, name: 'foo' });

  wrapper
    .find('SnippetViewer')
    .first()
    .prop<Function>('renderDuplicationPopup')(1, 13);
  expect(renderDuplicationPopup).toHaveBeenCalledWith(
    mockSourceViewerFile({ key: 'a', path: 'a' }),
    1,
    13
  );
});

describe('getNodes', () => {
  const snippetGroup: T.SnippetGroup = {
    component: mockSourceViewerFile(),
    locations: [],
    sources: []
  };
  const wrapper = mount<ComponentSourceSnippetViewer>(
    <ComponentSourceSnippetViewer
      branchLike={mockMainBranch()}
      duplications={undefined}
      duplicationsByLine={undefined}
      highlightedLocationMessage={{ index: 0, text: '' }}
      issue={mockIssue()}
      issuesByLine={{}}
      last={false}
      linePopup={undefined}
      loadDuplications={jest.fn()}
      locations={[]}
      onIssueChange={jest.fn()}
      onIssuePopupToggle={jest.fn()}
      onLinePopupToggle={jest.fn()}
      onLocationSelect={jest.fn()}
      renderDuplicationPopup={jest.fn()}
      scroll={jest.fn()}
      snippetGroup={snippetGroup}
    />
  );

  it('should return undefined if any node is missing', async () => {
    await waitAndUpdate(wrapper);
    const rootNode = wrapper.instance().rootNodeRef;
    mockDom(rootNode.current!);
    expect(wrapper.instance().getNodes(0)).toBeUndefined();
    expect(wrapper.instance().getNodes(1)).toBeUndefined();
    expect(wrapper.instance().getNodes(2)).toBeUndefined();
  });

  it('should return elements if dom is correct', async () => {
    await waitAndUpdate(wrapper);
    const rootNode = wrapper.instance().rootNodeRef;
    mockDom(rootNode.current!);
    expect(wrapper.instance().getNodes(3)).not.toBeUndefined();
  });
});

describe('getHeight', () => {
  jest.useFakeTimers();

  const snippetGroup: T.SnippetGroup = {
    component: mockSourceViewerFile(),
    locations: [],
    sources: []
  };
  const wrapper = mount<ComponentSourceSnippetViewer>(
    <ComponentSourceSnippetViewer
      branchLike={mockMainBranch()}
      duplications={undefined}
      duplicationsByLine={undefined}
      highlightedLocationMessage={{ index: 0, text: '' }}
      issue={mockIssue()}
      issuesByLine={{}}
      last={false}
      linePopup={undefined}
      loadDuplications={jest.fn()}
      locations={[]}
      onIssueChange={jest.fn()}
      onIssuePopupToggle={jest.fn()}
      onLinePopupToggle={jest.fn()}
      onLocationSelect={jest.fn()}
      renderDuplicationPopup={jest.fn()}
      scroll={jest.fn()}
      snippetGroup={snippetGroup}
    />
  );

  it('should set maxHeight to current height', async () => {
    await waitAndUpdate(wrapper);

    const nodes = mockDomForSizes(wrapper, { wrapperHeight: 42, tableHeight: 68 });
    wrapper.instance().setMaxHeight(0);

    expect(nodes.wrapper.getAttribute('style')).toBe('max-height: 88px;');
    expect(nodes.table.getAttribute('style')).toBeNull();
  });

  it('should set margin and then maxHeight for a nice upwards animation', async () => {
    await waitAndUpdate(wrapper);

    const nodes = mockDomForSizes(wrapper, { wrapperHeight: 42, tableHeight: 68 });
    wrapper.instance().setMaxHeight(0, undefined, true);

    expect(nodes.wrapper.getAttribute('style')).toBeNull();
    expect(nodes.table.getAttribute('style')).toBe('transition: none; margin-top: -26px;');

    jest.runAllTimers();

    expect(nodes.wrapper.getAttribute('style')).toBe('max-height: 88px;');
    expect(nodes.table.getAttribute('style')).toBe('margin-top: 0px;');
  });
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
      duplications={undefined}
      duplicationsByLine={undefined}
      highlightedLocationMessage={{ index: 0, text: '' }}
      issue={mockIssue()}
      issuesByLine={{}}
      last={false}
      linePopup={undefined}
      loadDuplications={jest.fn()}
      locations={[]}
      onIssueChange={jest.fn()}
      onIssuePopupToggle={jest.fn()}
      onLinePopupToggle={jest.fn()}
      onLocationSelect={jest.fn()}
      renderDuplicationPopup={jest.fn()}
      scroll={jest.fn()}
      snippetGroup={snippetGroup}
      {...props}
    />
  );
}

function mockDom(refNode: HTMLDivElement) {
  refNode.querySelector = jest.fn(query => {
    const index = query.split('-').pop();

    switch (index) {
      case '0':
        return null;
      case '1':
        return mount(<div />).getDOMNode();
      case '2':
        return mount(
          <div>
            <div className="snippet" />
          </div>
        ).getDOMNode();
      case '3':
        return mount(
          <div>
            <div className="snippet">
              <div />
            </div>
          </div>
        ).getDOMNode();
      default:
        return null;
    }
  });
}

function mockDomForSizes(
  componentWrapper: ReactWrapper<{}, {}, ComponentSourceSnippetViewer>,
  { wrapperHeight = 0, tableHeight = 0 }
) {
  const wrapper = mount(<div className="snippet" />).getDOMNode();
  wrapper.getBoundingClientRect = jest.fn().mockReturnValue({ height: wrapperHeight });
  const table = mount(<div />).getDOMNode();
  table.getBoundingClientRect = jest.fn().mockReturnValue({ height: tableHeight });
  componentWrapper.instance().getNodes = jest.fn().mockReturnValue({
    wrapper,
    table
  });
  return { wrapper, table };
}
