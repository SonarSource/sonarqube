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
import globalMessagesReducer, { MessageLevel } from '../globalMessages';

describe('globalMessagesReducer', () => {
  it('should handle ADD_GLOBAL_MESSAGE', () => {
    const actionAttributes = { id: 'id', message: 'There was an error', level: MessageLevel.Error };

    expect(
      globalMessagesReducer([], {
        type: 'ADD_GLOBAL_MESSAGE',
        ...actionAttributes
      })
    ).toEqual([actionAttributes]);
  });

  it('should handle CLOSE_GLOBAL_MESSAGE', () => {
    const state = [
      { id: 'm1', message: 'message 1', level: MessageLevel.Success },
      { id: 'm2', message: 'message 2', level: MessageLevel.Success }
    ];

    expect(globalMessagesReducer(state, { type: 'CLOSE_GLOBAL_MESSAGE', id: 'm2' })).toEqual([
      state[0]
    ]);
  });
});
