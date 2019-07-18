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
import { mockIssue, mockShortLivingBranch, mockSourceLine } from '../../../../helpers/testMocks';
import LineCode from '../LineCode';

it('render code', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(props: Partial<LineCode['props']> = {}) {
  return shallow(
    <LineCode
      branchLike={mockShortLivingBranch()}
      displayLocationMarkers={true}
      highlightedLocationMessage={{ index: 0, text: 'location description' }}
      highlightedSymbols={['sym-9']}
      issueLocations={[{ from: 0, to: 5, line: 16 }]}
      issuePopup={undefined}
      issues={[mockIssue(false, { key: 'issue-1' }), mockIssue(false, { key: 'issue-2' })]}
      line={mockSourceLine()}
      onIssueChange={jest.fn()}
      onIssuePopupToggle={jest.fn()}
      onIssueSelect={jest.fn()}
      onLocationSelect={jest.fn()}
      onSymbolClick={jest.fn()}
      secondaryIssueLocations={[]}
      selectedIssue="issue-1"
      showIssues={true}
      {...props}
    />
  );
}
