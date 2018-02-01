/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import MeasureCell from '../MeasureCell';

describe('should correctly take the value', () => {
  const renderAndTakeValue = props =>
    shallow(<MeasureCell {...props} />)
      .find('Measure')
      .prop('value');

  it('absolute value', () => {
    const component = { value: '123' };
    const metric = { key: 'coverage' };
    const measure = { value: '567' };

    expect(renderAndTakeValue({ component, metric })).toEqual('123');
    expect(renderAndTakeValue({ component, metric, measure })).toEqual('567');
  });

  it('leak value', () => {
    const component = { leak: '234' };
    const metric = { key: 'new_coverage' };
    const measure = { leak: '678' };

    expect(renderAndTakeValue({ component, metric })).toEqual('234');
    expect(renderAndTakeValue({ component, metric, measure })).toEqual('678');
  });
});
