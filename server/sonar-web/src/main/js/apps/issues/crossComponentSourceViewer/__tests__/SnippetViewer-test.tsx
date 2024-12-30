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

import { screen } from '@testing-library/react';
import { range } from 'lodash';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { mockSourceLine, mockSourceViewerFile } from '../../../../helpers/mocks/sources';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import SnippetViewer, { SnippetViewerProps } from '../SnippetViewer';

beforeEach(() => {
  jest.clearAllMocks();
});

const ui = {
  expandAbove: byRole('button', { name: 'source_viewer.expand_above' }),
  expandBelow: byRole('button', { name: 'source_viewer.expand_below' }),
  scmInfo: byRole('button', {
    name: 'source_viewer.author_X.simon.brandhof@sonarsource.com, source_viewer.click_for_scm_info.5',
  }),
};

it('should render correctly', () => {
  const snippet = range(5, 8).map((line) => mockSourceLine({ line }));
  renderSnippetViewer({
    snippet,
  });

  expect(ui.expandAbove.get()).toBeInTheDocument();
  expect(ui.expandBelow.get()).toBeInTheDocument();
  expect(ui.scmInfo.get()).toBeInTheDocument();
});

it('should render correctly when at the top of the file', () => {
  const snippet = range(1, 8).map((line) => mockSourceLine({ line }));
  renderSnippetViewer({
    snippet,
  });

  expect(ui.expandAbove.query()).not.toBeInTheDocument();
  expect(ui.expandBelow.get()).toBeInTheDocument();
});

it('should render correctly when at the bottom of the file', () => {
  const component = mockSourceViewerFile('foo/bar.ts', 'my-project', { measures: { lines: '14' } });
  const snippet = range(10, 15).map((line) => mockSourceLine({ line }));
  renderSnippetViewer({
    component,
    snippet,
  });

  expect(ui.expandAbove.get()).toBeInTheDocument();
  expect(ui.expandBelow.query()).not.toBeInTheDocument();
});

it('should render correctly with no SCM', () => {
  const snippet = range(5, 8).map((line) => mockSourceLine({ line }));
  renderSnippetViewer({
    displaySCM: false,
    snippet,
  });

  expect(ui.scmInfo.query()).not.toBeInTheDocument();
});

it('should render additional child in line', () => {
  const sourceline = mockSourceLine({ line: 42 });

  const child = <div data-testid="additional-child">child</div>;
  const renderAdditionalChildInLine = jest.fn().mockReturnValue(child);
  renderSnippetViewer({ renderAdditionalChildInLine, snippet: [sourceline] });

  expect(screen.getByTestId('additional-child')).toBeInTheDocument();
});

function renderSnippetViewer(props: Partial<SnippetViewerProps> = {}) {
  return renderComponent(
    <SnippetViewer
      component={mockSourceViewerFile()}
      duplications={undefined}
      duplicationsByLine={undefined}
      expandBlock={jest.fn()}
      handleSymbolClick={jest.fn()}
      highlightedLocationMessage={{ index: 0, text: '' }}
      highlightedSymbols={[]}
      index={0}
      loadDuplications={jest.fn()}
      locations={[]}
      locationsByLine={{}}
      onLocationSelect={jest.fn()}
      renderDuplicationPopup={jest.fn()}
      snippet={[]}
      {...props}
    />,
  );
}
