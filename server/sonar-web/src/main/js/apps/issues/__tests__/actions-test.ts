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

import { mockIssue } from '../../../helpers/testMocks';
import { selectFlow, selectLocation } from '../actions';

describe('selectFlow', () => {
  it('should select flow and enable locations navigator', () => {
    expect(selectFlow(5)()).toEqual({
      locationsNavigator: true,
      selectedFlowIndex: 5,
      selectedLocationIndex: 0
    });
  });
});

describe('selectLocation', () => {
  it('should select location and enable locations navigator', () => {
    expect(selectLocation(5)({ openIssue: mockIssue() })).toEqual({
      locationsNavigator: true,
      selectedLocationIndex: 5
    });
  });

  it('should deselect location when clicked again', () => {
    expect(selectLocation(5)({ openIssue: mockIssue(), selectedLocationIndex: 5 })).toEqual({
      locationsNavigator: false,
      selectedLocationIndex: undefined
    });
  });

  it('should ignore if no open issue', () => {
    expect(selectLocation(5)({ openIssue: undefined })).toBeNull();
  });
});
