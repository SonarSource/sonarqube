/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { connect } from 'react-redux';
import '../AppContainer';

jest.mock('react-redux', () => ({
  connect: jest.fn(() => (a: any) => a)
}));

jest.mock('../../../../store/rootReducer', () => {
  return {
    getLanguages: jest.fn(() => [
      { key: 'css', name: 'CSS' },
      { key: 'js', name: 'JS' }
    ])
  };
});

describe('redux', () => {
  it('should correctly map state and dispatch props', () => {
    const [mapStateToProps] = (connect as jest.Mock).mock.calls[0];
    const { languages } = mapStateToProps({});

    expect(languages).toEqual([
      { key: 'css', name: 'CSS' },
      { key: 'js', name: 'JS' }
    ]);
  });
});
