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
import { shallow, mount } from 'enzyme';
import sinon from 'sinon';
import MultiValueInput from '../MultiValueInput';
import InputForString from '../InputForString';
import { click, change } from '../../../../../../../../tests/utils';

const definition = { multiValues: true };

const assertValues = (inputs, values) => {
  values.forEach((value, index) => {
    const input = inputs.at(index);
    expect(input.prop('value')).to.equal(value);
  });
};

describe('Settings :: Inputs :: MultiValueInput', () => {
  it('should render one value', () => {
    const multiValueInput = mount(
        <MultiValueInput
            setting={{ definition }}
            value={['foo']}
            onChange={sinon.stub()}/>
    );
    const stringInputs = multiValueInput.find(InputForString);
    expect(stringInputs).to.have.length(1 + 1);
    assertValues(stringInputs, ['foo', '']);
  });

  it('should render several values', () => {
    const multiValueInput = mount(
        <MultiValueInput
            setting={{ definition }}
            value={['foo', 'bar', 'baz']}
            onChange={sinon.stub()}/>
    );
    const stringInputs = multiValueInput.find(InputForString);
    expect(stringInputs).to.have.length(3 + 1);
    assertValues(stringInputs, ['foo', 'bar', 'baz', '']);
  });

  it('should remove value', () => {
    const onChange = sinon.spy();
    const multiValueInput = mount(
        <MultiValueInput
            setting={{ definition }}
            value={['foo', 'bar', 'baz']}
            onChange={onChange}/>
    );

    click(multiValueInput.find('.js-remove-value').at(1));
    expect(onChange.called).to.equal(true);
    expect(onChange.lastCall.args).to.deep.equal([['foo', 'baz']]);
  });

  it('should change existing value', () => {
    const onChange = sinon.spy();
    const multiValueInput = mount(
        <MultiValueInput
            setting={{ definition }}
            value={['foo', 'bar', 'baz']}
            onChange={onChange}/>
    );

    change(multiValueInput.find(InputForString).at(1).find('input'), 'qux');
    expect(onChange.called).to.equal(true);
    expect(onChange.lastCall.args).to.deep.equal([['foo', 'qux', 'baz']]);
  });

  it('should add new value', () => {
    const onChange = sinon.spy();
    const multiValueInput = mount(
        <MultiValueInput
            setting={{ definition }}
            value={['foo']}
            onChange={onChange}/>
    );

    change(multiValueInput.find(InputForString).at(1).find('input'), 'bar');
    expect(onChange.called).to.equal(true);
    expect(onChange.lastCall.args).to.deep.equal([['foo', 'bar']]);
  });
});
