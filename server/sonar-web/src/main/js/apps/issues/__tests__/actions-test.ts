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
import { mockIssue } from '../../../helpers/testMocks';
import { enableLocationsNavigator, selectFlow } from '../actions';
import { State } from '../components/IssuesApp';

describe('selectFlow', () => {
  it('should select flow and enable locations navigator', () => {
    expect(selectFlow(5)()).toEqual({
      locationsNavigator: true,
      selectedFlowIndex: 5,
      selectedLocationIndex: undefined,
    });
  });
});

describe('enableLocationsNavigator', () => {
  it('should compute the correct flow index', () => {
    expect(
      enableLocationsNavigator({ openIssue: mockIssue(true), selectedFlowIndex: 20 } as State),
    ).toEqual(
      expect.objectContaining({
        locationsNavigator: true,
        selectedFlowIndex: 20,
      }),
    );
    expect(enableLocationsNavigator({ openIssue: mockIssue(true) } as State)).toEqual(
      expect.objectContaining({
        locationsNavigator: true,
        selectedFlowIndex: 0,
      }),
    );
    expect(
      enableLocationsNavigator({ openIssue: mockIssue(true, { flows: [] }) } as State),
    ).toEqual(
      expect.objectContaining({
        locationsNavigator: true,
        selectedFlowIndex: undefined,
      }),
    );
  });

  it('should compute the correct selected location index', () => {
    expect(enableLocationsNavigator({ openIssue: mockIssue(true) } as State)).toEqual(
      expect.objectContaining({
        locationsNavigator: true,
        selectedLocationIndex: undefined,
      }),
    );

    expect(
      enableLocationsNavigator({ openIssue: mockIssue(true), selectedLocationIndex: -1 } as State),
    ).toEqual(
      expect.objectContaining({
        locationsNavigator: true,
        selectedLocationIndex: 0,
      }),
    );

    expect(
      enableLocationsNavigator({ openIssue: mockIssue(true), selectedLocationIndex: 20 } as State),
    ).toEqual(
      expect.objectContaining({
        locationsNavigator: true,
        selectedLocationIndex: 20,
      }),
    );
  });

  it('should do nothing if the open issue has no secondary locations or any flows', () => {
    expect(enableLocationsNavigator({ openIssue: mockIssue() } as State)).toBeNull();
    expect(
      enableLocationsNavigator({
        openIssue: mockIssue(true, { flows: [], secondaryLocations: [] }),
      } as State),
    ).toBeNull();
  });

  it('should do nothing if there is no open issue', () => {
    expect(enableLocationsNavigator({} as State)).toBeNull();
  });
});
