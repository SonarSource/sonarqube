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
import { mockBranch } from '../../../helpers/mocks/branch-like';
import { mockSourceLine } from '../../../helpers/mocks/sources';
import { mockIssue } from '../../../helpers/testMocks';
import { MetricKey } from '../../../types/metrics';
import Line from '../components/Line';
import SourceViewerCode from '../SourceViewerCode';

it('should correctly flag a line for scrolling', () => {
  const sources = [
    mockSourceLine({ line: 1, coverageStatus: 'covered', isNew: false }),
    mockSourceLine({ line: 2, coverageStatus: 'partially-covered', isNew: false }),
    mockSourceLine({ line: 3, coverageStatus: 'uncovered', isNew: true }),
  ];
  let wrapper = shallowRender({ sources, metricKey: MetricKey.uncovered_lines });

  expect(wrapper.find(Line).at(1).props().scrollToUncoveredLine).toBe(true);
  expect(wrapper.find(Line).at(2).props().scrollToUncoveredLine).toBe(false);

  wrapper = shallowRender({
    sources,
    metricKey: MetricKey.new_uncovered_lines,
  });

  expect(wrapper.find(Line).at(1).props().scrollToUncoveredLine).toBe(false);
  expect(wrapper.find(Line).at(2).props().scrollToUncoveredLine).toBe(true);
});

function shallowRender(props: Partial<SourceViewerCode['props']> = {}) {
  return shallow<SourceViewerCode>(
    <SourceViewerCode
      branchLike={mockBranch()}
      duplications={[]}
      duplicationsByLine={[]}
      hasSourcesAfter={false}
      hasSourcesBefore={false}
      highlightedLine={undefined}
      highlightedLocationMessage={undefined}
      highlightedLocations={undefined}
      highlightedSymbols={[]}
      issueLocationsByLine={{}}
      issuePopup={undefined}
      issues={[mockIssue(), mockIssue()]}
      issuesByLine={{}}
      loadDuplications={jest.fn()}
      loadingSourcesAfter={false}
      loadingSourcesBefore={false}
      loadSourcesAfter={jest.fn()}
      loadSourcesBefore={jest.fn()}
      onIssueChange={jest.fn()}
      onIssuePopupToggle={jest.fn()}
      onIssuesClose={jest.fn()}
      onIssueSelect={jest.fn()}
      onIssuesOpen={jest.fn()}
      onIssueUnselect={jest.fn()}
      onLocationSelect={jest.fn()}
      onSymbolClick={jest.fn()}
      openIssuesByLine={{}}
      renderDuplicationPopup={jest.fn()}
      selectedIssue={undefined}
      sources={[mockSourceLine(), mockSourceLine(), mockSourceLine()]}
      symbolsByLine={{}}
      {...props}
    />
  );
}
