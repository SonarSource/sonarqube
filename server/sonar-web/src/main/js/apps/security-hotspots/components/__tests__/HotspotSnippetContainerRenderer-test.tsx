/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { scrollToElement } from '../../../../helpers/scrolling';
import { mockSourceLine, mockSourceViewerFile } from '../../../../helpers/testMocks';
import SnippetViewer from '../../../issues/crossComponentSourceViewer/SnippetViewer';
import HotspotSnippetContainerRenderer, {
  getScrollHandler,
  HotspotSnippetContainerRendererProps
} from '../HotspotSnippetContainerRenderer';

jest.mock('../../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn()
}));

beforeEach(() => {
  jest.spyOn(React, 'useMemo').mockImplementationOnce(f => f());
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ sourceLines: [mockSourceLine()] })).toMatchSnapshot('with sourcelines');
});

it('should render a HotspotPrimaryLocationBox', () => {
  const wrapper = shallowRender({
    hotspot: mockHotspot({ line: 42 }),
    sourceLines: [mockSourceLine()]
  });

  const { renderAdditionalChildInLine } = wrapper.find(SnippetViewer).props();

  expect(renderAdditionalChildInLine!(10)).toBeUndefined();
  expect(renderAdditionalChildInLine!(42)).not.toBeUndefined();
});

it('should render correctly when secondary location is selected', () => {
  const wrapper = shallowRender({
    selectedHotspotLocation: 1
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
      current: document.createElement('div')
    };

    const scrollHandler = getScrollHandler(ref);

    const targetElement = document.createElement('div');

    scrollHandler(targetElement);
    jest.runAllTimers();

    expect(scrollToElement).toBeCalled();
  });

  it('should not scroll if parent is undefined', () => {
    const ref: RefObject<HTMLDivElement> = {
      current: null
    };

    const scrollHandler = getScrollHandler(ref);

    const targetElement = document.createElement('div');

    scrollHandler(targetElement);
    jest.runAllTimers();

    expect(scrollToElement).not.toBeCalled();
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
