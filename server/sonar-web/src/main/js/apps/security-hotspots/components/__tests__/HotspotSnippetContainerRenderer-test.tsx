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
import React, { RefObject } from 'react';
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockHotspot } from '../../../../helpers/mocks/security-hotspots';
import { mockSourceLine, mockSourceViewerFile } from '../../../../helpers/mocks/sources';
import { scrollToElement } from '../../../../helpers/scrolling';
import SnippetViewer from '../../../issues/crossComponentSourceViewer/SnippetViewer';
import HotspotSnippetContainerRenderer, {
  animateExpansion,
  getScrollHandler,
  HotspotSnippetContainerRendererProps,
} from '../HotspotSnippetContainerRenderer';

jest.mock('../../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn(),
}));

beforeEach(() => {
  jest.spyOn(React, 'useMemo').mockImplementationOnce((f) => f());
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ sourceLines: [mockSourceLine()] })).toMatchSnapshot('with sourcelines');
});

it('should render a HotspotPrimaryLocationBox', () => {
  const wrapper = shallowRender({
    hotspot: mockHotspot({ line: 42 }),
    sourceLines: [mockSourceLine()],
  });

  const { renderAdditionalChildInLine } = wrapper.find(SnippetViewer).props();

  expect(renderAdditionalChildInLine!(mockSourceLine({ line: 10 }))).toBeUndefined();
  expect(renderAdditionalChildInLine!(mockSourceLine({ line: 42 }))).not.toBeUndefined();
});

it('should render correctly when secondary location is selected', () => {
  const wrapper = shallowRender({
    selectedHotspotLocation: 1,
  });
  expect(wrapper).toMatchSnapshot('with selected hotspot location');
});

describe('scrolling', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should scroll to element if parent is defined', () => {
    const ref: RefObject<HTMLDivElement> = {
      current: document.createElement('div'),
    };

    const scrollHandler = getScrollHandler(ref);

    const targetElement = document.createElement('div');

    scrollHandler(targetElement);
    jest.runAllTimers();

    expect(scrollToElement).toHaveBeenCalled();
  });

  it('should not scroll if parent is undefined', () => {
    const ref: RefObject<HTMLDivElement> = {
      current: null,
    };

    const scrollHandler = getScrollHandler(ref);

    const targetElement = document.createElement('div');

    scrollHandler(targetElement);
    jest.runAllTimers();

    expect(scrollToElement).not.toHaveBeenCalled();
  });
});

describe('expand', () => {
  it('should work as expected', async () => {
    jest.useFakeTimers();
    const onExpandBlock = jest.fn().mockResolvedValue({});

    const scrollableNode = document.createElement('div');
    scrollableNode.scrollTo = jest.fn();
    const ref: RefObject<HTMLDivElement> = {
      current: scrollableNode,
    };

    jest.spyOn(React, 'useRef').mockReturnValue(ref);

    const snippet = document.createElement('div');
    const table = document.createElement('table');
    snippet.appendChild(table);
    scrollableNode.querySelector = jest.fn().mockReturnValue(snippet);

    jest
      .spyOn(table, 'getBoundingClientRect')
      .mockReturnValueOnce({ height: 42 } as DOMRect)
      .mockReturnValueOnce({ height: 99 } as DOMRect)
      .mockReturnValueOnce({ height: 99 } as DOMRect)
      .mockReturnValueOnce({ height: 112 } as DOMRect);

    await animateExpansion(ref, onExpandBlock, 'up');
    expect(onExpandBlock).toHaveBeenCalledWith('up');

    expect(snippet).toHaveStyle({ maxHeight: '42px' });
    expect(table).toHaveStyle({ marginTop: '-57px' });

    jest.advanceTimersByTime(100);

    expect(snippet).toHaveStyle({ maxHeight: '99px' });
    expect(table).toHaveStyle({ marginTop: '0px' });

    expect(scrollableNode.scrollTo).not.toHaveBeenCalled();

    jest.runAllTimers();

    await animateExpansion(ref, onExpandBlock, 'down');
    expect(onExpandBlock).toHaveBeenCalledWith('down');
    expect(snippet).toHaveStyle({ maxHeight: '112px' });

    jest.useRealTimers();
  });
});

function shallowRender(props?: Partial<HotspotSnippetContainerRendererProps>) {
  return shallow(
    <HotspotSnippetContainerRenderer
      branchLike={mockMainBranch()}
      highlightedSymbols={[]}
      hotspot={mockHotspot()}
      loading={false}
      locations={{}}
      onCommentButtonClick={jest.fn()}
      onExpandBlock={jest.fn()}
      onSymbolClick={jest.fn()}
      secondaryLocations={[]}
      sourceLines={[]}
      sourceViewerFile={mockSourceViewerFile()}
      component={mockComponent()}
      onLocationSelect={jest.fn()}
      {...props}
    />
  );
}
