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
import { mockSourceLine, mockSourceViewerFile } from '../../../../helpers/mocks/sources';
import { mockIssue } from '../../../../helpers/testMocks';
import SnippetViewer from '../SnippetViewer';

jest.mock('../../../../helpers/scrolling', () => ({
  scrollHorizontally: jest.fn(),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  const snippet = range(5, 8).map((line) => mockSourceLine({ line }));
  const wrapper = shallowRender({
    snippet,
  });

  expect(wrapper).toMatchSnapshot();
});

it('should render correctly with no SCM', () => {
  const snippet = range(5, 8).map((line) => mockSourceLine({ line }));
  const wrapper = shallowRender({
    displaySCM: false,
    snippet,
  });

  expect(wrapper).toMatchSnapshot();
});

it('should render additional child in line', () => {
  const sourceline = mockSourceLine({ line: 42 });

  const child = <div>child</div>;
  const renderAdditionalChildInLine = jest.fn().mockReturnValue(child);
  const wrapper = shallowRender({ renderAdditionalChildInLine, snippet: [sourceline] });

  wrapper.instance().renderLine({
    displayDuplications: false,
    index: 1,
    issueLocations: [],
    line: sourceline,
    snippet: [sourceline],
    symbols: [],
    verticalBuffer: 5,
  });

  expect(renderAdditionalChildInLine).toHaveBeenCalledWith(sourceline);
});

it('should render correctly when at the top of the file', () => {
  const snippet = range(1, 8).map((line) => mockSourceLine({ line }));
  const wrapper = shallowRender({
    snippet,
  });

  expect(wrapper).toMatchSnapshot();
});

it('should render correctly when at the bottom of the file', () => {
  const component = mockSourceViewerFile('foo/bar.ts', 'my-project', { measures: { lines: '14' } });
  const snippet = range(10, 14).map((line) => mockSourceLine({ line }));
  const wrapper = shallowRender({
    component,
    snippet,
  });

  expect(wrapper).toMatchSnapshot();
});

it('should correctly handle expansion', () => {
  const snippet = range(5, 8).map((line) => mockSourceLine({ line }));
  const expandBlock = jest.fn(() => Promise.resolve());

  const wrapper = shallowRender({
    expandBlock,
    index: 2,
    snippet,
  });

  wrapper.find('.expand-block-above button').first().simulate('click');
  expect(expandBlock).toHaveBeenCalledWith(2, 'up');

  wrapper.find('.expand-block-below button').first().simulate('click');
  expect(expandBlock).toHaveBeenCalledWith(2, 'down');
});

function shallowRender(props: Partial<SnippetViewer['props']> = {}) {
  return shallow<SnippetViewer>(
    <SnippetViewer
      component={mockSourceViewerFile()}
      duplications={undefined}
      duplicationsByLine={undefined}
      expandBlock={jest.fn()}
      handleSymbolClick={jest.fn()}
      highlightedLocationMessage={{ index: 0, text: '' }}
      highlightedSymbols={[]}
      index={0}
      issue={mockIssue()}
      lastSnippetOfLastGroup={false}
      loadDuplications={jest.fn()}
      locations={[]}
      locationsByLine={{}}
      onLocationSelect={jest.fn()}
      renderDuplicationPopup={jest.fn()}
      snippet={[]}
      {...props}
    />
  );
}
