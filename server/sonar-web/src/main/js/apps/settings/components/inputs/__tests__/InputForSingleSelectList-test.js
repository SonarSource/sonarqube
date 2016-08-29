/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { expect } from 'chai';
import { shallow } from 'enzyme';
import sinon from 'sinon';
import Select from 'react-select';
import InputForSingleSelectList from '../InputForSingleSelectList';

describe('Settings :: Inputs :: InputForSingleSelectList', () => {
  it('should render Select', () => {
    const onChange = sinon.spy();
    const select = shallow(
        <InputForSingleSelectList
            name="foo"
            value="bar"
            options={['foo', 'bar', 'baz']}
            isDefault={false}
            onChange={onChange}/>
    ).find(Select);
    expect(select).to.have.length(1);
    expect(select.prop('name')).to.equal('foo');
    expect(select.prop('value')).to.equal('bar');
    expect(select.prop('options')).to.deep.equal([
      { value: 'foo', label: 'foo' },
      { value: 'bar', label: 'bar' },
      { value: 'baz', label: 'baz' }
    ]);
    expect(select.prop('onChange')).to.be.a('function');
  });

  it('should call onChange', () => {
    const onChange = sinon.spy();
    const select = shallow(
        <InputForSingleSelectList
            name="foo"
            value="bar"
            options={['foo', 'bar', 'baz']}
            isDefault={false}
            onChange={onChange}/>
    ).find(Select);
    expect(select).to.have.length(1);
    expect(select.prop('onChange')).to.be.a('function');

    select.prop('onChange')({ value: 'baz', label: 'baz' });
    expect(onChange.called).to.equal(true);
    expect(onChange.lastCall.args).to.deep.equal([undefined, 'baz']);
  });
});
