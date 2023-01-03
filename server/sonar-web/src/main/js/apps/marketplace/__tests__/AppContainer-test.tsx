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
import { mockStore } from '../../../helpers/testMocks';
import { getAppState, getGlobalSettingValue } from '../../../store/rootReducer';
import { EditionKey } from '../../../types/editions';
import '../AppContainer';

jest.mock('react-redux', () => ({
  connect: jest.fn(() => (a: any) => a)
}));

jest.mock('../../../store/rootReducer', () => {
  return {
    getAppState: jest.fn(),
    getGlobalSettingValue: jest.fn()
  };
});

describe('redux', () => {
  it('should correctly map state and dispatch props', () => {
    const store = mockStore();
    const edition = EditionKey.developer;
    const standalone = true;
    const updateCenterActive = true;
    (getAppState as jest.Mock).mockReturnValue({ edition, standalone });
    (getGlobalSettingValue as jest.Mock).mockReturnValueOnce({
      value: `${updateCenterActive}`
    });

    const [mapStateToProps] = (connect as jest.Mock).mock.calls[0];

    const props = mapStateToProps(store);
    expect(props).toEqual({
      currentEdition: edition,
      standaloneMode: standalone,
      updateCenterActive
    });

    expect(getGlobalSettingValue).toHaveBeenCalledWith(store, 'sonar.updatecenter.activate');
  });
});
