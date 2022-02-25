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
import { fetchValues, setValues } from '../actions';

jest.mock('../../../../api/settings', () => {
  const { mockSettingValue } = jest.requireActual('../../../../helpers/mocks/settings');
  return {
    getValues: jest.fn().mockResolvedValue([mockSettingValue()])
  };
});

it('should setValues correctly', () => {
  const dispatch = jest.fn();
  setValues(['test'], [{ key: 'test', value: 'foo' }])(dispatch);
  expect(dispatch).toHaveBeenCalledWith({
    component: undefined,
    settings: [
      {
        key: 'test',
        value: 'foo'
      }
    ],
    type: 'RECEIVE_VALUES',
    updateKeys: ['test']
  });
});

it('should fetchValue correclty', async () => {
  const dispatch = jest.fn();
  await fetchValues(['test'], 'foo')(dispatch);
  expect(dispatch).toHaveBeenCalledWith({
    component: 'foo',
    settings: [{ key: 'test' }],
    type: 'RECEIVE_VALUES',
    updateKeys: ['test']
  });
  expect(dispatch).toHaveBeenCalledWith({ type: 'CLOSE_ALL_GLOBAL_MESSAGES' });
});
