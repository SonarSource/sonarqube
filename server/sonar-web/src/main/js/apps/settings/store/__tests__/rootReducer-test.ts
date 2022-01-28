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
import { getSettingsForCategory } from '../rootReducer';

it('Should correclty assert if value is set', () => {
  const settings = getSettingsForCategory(
    {
      definitions: {
        foo: { category: 'cat', key: 'foo', fields: [], options: [], subCategory: 'test' },
        bar: { category: 'cat', key: 'bar', fields: [], options: [], subCategory: 'test' }
      },
      globalMessages: [],
      settingsPage: {
        changedValues: {},
        loading: {},
        validationMessages: {}
      },
      values: { components: {}, global: { foo: { key: 'foo' } } }
    },
    'cat'
  );
  expect(settings[0].hasValue).toBe(true);
  expect(settings[1].hasValue).toBe(false);
});
