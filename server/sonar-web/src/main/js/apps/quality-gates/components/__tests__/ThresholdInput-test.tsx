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
import * as React from 'react';
import { shallow } from 'enzyme';
import ThresholdInput from '../ThresholdInput';
import { change } from '../../../../helpers/testUtils';

describe('on strings', () => {
  const metric = { id: '1', key: 'foo', name: 'Foo', type: 'INTEGER' };
  it('should render text input', () => {
    const input = shallow(
      <ThresholdInput metric={metric} name="foo" onChange={jest.fn()} value="2" />
    ).find('input');
    expect(input.length).toEqual(1);
    expect(input.prop('name')).toEqual('foo');
    expect(input.prop('value')).toEqual('2');
  });

  it('should change', () => {
    const onChange = jest.fn();
    const input = shallow(
      <ThresholdInput metric={metric} name="foo" onChange={onChange} value="2" />
    ).find('input');
    change(input, 'bar');
    expect(onChange).toBeCalledWith('bar');
  });
});

describe('on ratings', () => {
  const metric = { id: '1', key: 'foo', name: 'Foo', type: 'RATING' };
  it('should render Select', () => {
    const select = shallow(
      <ThresholdInput metric={metric} name="foo" onChange={jest.fn()} value="2" />
    ).find('Select');
    expect(select.length).toEqual(1);
    expect(select.prop('value')).toEqual('2');
  });

  it('should set', () => {
    const onChange = jest.fn();
    const select = shallow(
      <ThresholdInput metric={metric} name="foo" onChange={onChange} value="2" />
    ).find('Select');
    (select.prop('onChange') as Function)({ label: 'D', value: '4' });
    expect(onChange).toBeCalledWith('4');
  });

  it('should unset', () => {
    const onChange = jest.fn();
    const select = shallow(
      <ThresholdInput metric={metric} name="foo" onChange={onChange} value="2" />
    ).find('Select');
    (select.prop('onChange') as Function)(null);
    expect(onChange).toBeCalledWith('');
  });
});
