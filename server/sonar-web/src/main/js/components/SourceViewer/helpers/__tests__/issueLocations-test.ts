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
import { mockFlowLocation, mockSourceLine } from '../../../../helpers/testMocks';
import { getLinearLocations, getSecondaryIssueLocationsForLine } from '../issueLocations';

describe('getSecondaryIssueLocationsForLine', () => {
  it('should return secondary locations for a line', () => {
    const sourceLine = mockSourceLine({ line: 2 });
    expect(getSecondaryIssueLocationsForLine(sourceLine, undefined)).toEqual([]);
    expect(getSecondaryIssueLocationsForLine(sourceLine, [mockFlowLocation()])).toEqual([
      { from: 0, index: undefined, line: 2, startLine: 1, text: undefined, to: 2 }
    ]);
  });
});

describe('getLinearLocations', () => {
  it('should return a linear location', () => {
    expect(getLinearLocations({ startLine: 6, startOffset: 3, endLine: 8, endOffset: 56 })).toEqual(
      [
        { from: 3, line: 6, to: 999999 },
        { from: 0, line: 7, to: 999999 },
        { from: 0, line: 8, to: 56 }
      ]
    );
    expect(getLinearLocations({ startLine: 6, startOffset: 0, endLine: 6, endOffset: 42 })).toEqual(
      [{ from: 0, line: 6, to: 42 }]
    );
  });
});
