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
import { stringify } from '../stringify-queryparams';

describe('stringify', () => {
  it('should properly format query params object', () => {
    const obj = {
      prop1: 'a string',
      prop2: 123,
      prop3: true,
      prop4: '',
      prop5: [9, 8, 7],
      prop6: { test: 'test' },
    };

    expect(stringify(obj)).toEqual(
      'prop1=a%20string&prop2=123&prop3=true&prop4=&prop5=9&prop5=8&prop5=7&prop6=',
    );
  });

  it('should return empty if name is not defined', () => {
    expect(stringify('test_obj', undefined, undefined, undefined)).toEqual('');
  });

  it('should properly format a query param', () => {
    expect(stringify('test_obj', undefined, undefined, 'test_name')).toEqual('test_name=test_obj');
  });
});
