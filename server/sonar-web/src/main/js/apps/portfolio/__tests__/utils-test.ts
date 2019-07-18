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
import { mockMeasure } from '../../../helpers/testMocks';
import { convertMeasures } from '../utils';

describe('convertMeasures', () => {
  it('should correctly transform a list of metrics to a dictionary', () => {
    const metrics = [
      mockMeasure({ metric: 'bugs', value: '1' }),
      mockMeasure({ metric: 'vulnerabilities', value: undefined })
    ];
    expect(convertMeasures(metrics)).toEqual({
      bugs: '1',
      vulnerabilities: undefined
    });
  });
});
