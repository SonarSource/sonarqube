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
import { range } from 'lodash';
import * as React from 'react';
import { getSources } from '../../../../api/components';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockHotspot, mockHotspotComponent } from '../../../../helpers/mocks/security-hotspots';
import { mockSourceLine } from '../../../../helpers/mocks/sources';
import { mockFlowLocation } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import HotspotSnippetContainer from '../HotspotSnippetContainer';
import HotspotSnippetContainerRenderer from '../HotspotSnippetContainerRenderer';

jest.mock('../../../../api/components', () => ({
  getSources: jest.fn().mockResolvedValue([]),
}));

jest.mock('../../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn(),
}));

beforeEach(() => jest.clearAllMocks());

const branch = mockBranch();

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should load sources on mount', async () => {
  (getSources as jest.Mock).mockResolvedValueOnce(
    range(1, 25).map((line) => mockSourceLine({ line }))
  );

  const hotspot = mockHotspot({
    project: mockHotspotComponent({ branch: branch.name, qualifier: ComponentQualifier.Project }),
    textRange: { startLine: 10, endLine: 11, startOffset: 0, endOffset: 12 },
    flows: [
      {
        locations: [
          mockFlowLocation({
            textRange: { startLine: 8, endLine: 8, startOffset: 0, endOffset: 1 },
          }),
          mockFlowLocation({
            textRange: { startLine: 13, endLine: 13, startOffset: 0, endOffset: 1 },
          }),
        ],
      },
    ],
  });

  const wrapper = shallowRender({ hotspot });

  await waitAndUpdate(wrapper);

  expect(getSources).toHaveBeenCalledWith(
    expect.objectContaining({
      key: hotspot.component.key,
      branch: branch.name,
      from: 1,
      to: 24,
    })
  );
  expect(wrapper.state().lastLine).toBeUndefined();
  expect(wrapper.state().sourceLines).toHaveLength(23);
});

it('should handle load sources failure', async () => {
  (getSources as jest.Mock).mockRejectedValueOnce(null);

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(getSources).toHaveBeenCalled();
  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().lastLine).toBeUndefined();
  expect(wrapper.state().sourceLines).toHaveLength(0);
});

it('should not load sources on mount when the hotspot is not associated to any loc', async () => {
  const hotspot = mockHotspot({
    line: undefined,
    textRange: undefined,
  });

  const wrapper = shallowRender({ hotspot });

  await waitAndUpdate(wrapper);

  expect(getSources).not.toHaveBeenCalled();
  expect(wrapper.state().lastLine).toBeUndefined();
  expect(wrapper.state().sourceLines).toHaveLength(0);
});

it('should handle end-of-file on mount', async () => {
  (getSources as jest.Mock).mockResolvedValueOnce(
    range(5, 15).map((line) => mockSourceLine({ line }))
  );

  const hotspot = mockHotspot({
    textRange: { startLine: 10, endLine: 11, startOffset: 0, endOffset: 12 },
  });

  const wrapper = shallowRender({ hotspot });

  await waitAndUpdate(wrapper);

  expect(getSources).toHaveBeenCalled();
  expect(wrapper.state().lastLine).toBe(14);
  expect(wrapper.state().sourceLines).toHaveLength(10);
});

describe('Expansion', () => {
  beforeEach(() => {
    (getSources as jest.Mock).mockResolvedValueOnce(
      range(10, 32).map((line) => mockSourceLine({ line }))
    );
  });

  const hotspot = mockHotspot({
    project: mockHotspotComponent({ branch: branch.name, qualifier: ComponentQualifier.Project }),
    textRange: { startLine: 20, endLine: 21, startOffset: 0, endOffset: 12 },
  });

  it('up should work', async () => {
    (getSources as jest.Mock).mockResolvedValueOnce(
      range(1, 10).map((line) => mockSourceLine({ line }))
    );

    const wrapper = shallowRender({ hotspot });
    await waitAndUpdate(wrapper);

    wrapper.find(HotspotSnippetContainerRenderer).props().onExpandBlock('up');

    await waitAndUpdate(wrapper);

    expect(getSources).toHaveBeenCalledWith(
      expect.objectContaining({
        branch: branch.name,
      })
    );
    expect(wrapper.state().sourceLines).toHaveLength(31);
  });

  it('down should work', async () => {
    (getSources as jest.Mock).mockResolvedValueOnce(
      // lastLine + expand + extra for EOF check + range end is excluded
      // 31 + 50 + 1 + 1
      range(32, 83).map((line) => mockSourceLine({ line }))
    );

    const wrapper = shallowRender({ hotspot });
    await waitAndUpdate(wrapper);

    wrapper.find(HotspotSnippetContainerRenderer).props().onExpandBlock('down');

    await waitAndUpdate(wrapper);

    expect(wrapper.state().lastLine).toBeUndefined();
    expect(wrapper.state().sourceLines).toHaveLength(72);
  });

  it('down should work and handle EOF', async () => {
    (getSources as jest.Mock).mockResolvedValueOnce(
      // lastLine + expand + extra for EOF check + range end is excluded - 1 to trigger end-of-file
      // 26 + 50 + 1 + 1 - 1
      range(27, 77).map((line) => mockSourceLine({ line }))
    );

    const wrapper = shallowRender({ hotspot });
    await waitAndUpdate(wrapper);

    wrapper.find(HotspotSnippetContainerRenderer).props().onExpandBlock('down');

    await waitAndUpdate(wrapper);

    expect(wrapper.state().lastLine).toBe(76);
    expect(wrapper.state().sourceLines).toHaveLength(72);
  });
});

it('should handle symbol click', () => {
  const wrapper = shallowRender();
  const symbols = ['symbol'];
  wrapper.find(HotspotSnippetContainerRenderer).props().onSymbolClick(symbols);
  expect(wrapper.state().highlightedSymbols).toBe(symbols);
});

function shallowRender(props?: Partial<HotspotSnippetContainer['props']>) {
  return shallow<HotspotSnippetContainer>(
    <HotspotSnippetContainer
      branchLike={branch}
      component={mockComponent()}
      hotspot={mockHotspot()}
      onCommentButtonClick={jest.fn()}
      onLocationSelect={jest.fn()}
      {...props}
    />
  );
}
