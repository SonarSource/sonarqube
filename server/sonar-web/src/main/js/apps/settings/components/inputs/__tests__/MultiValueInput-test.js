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
import { DEBOUNCE_WAIT } from '../../../constants';

const definition = { multiValues: true };

const assertValues = (inputs, values) => {
  values.forEach((value, index) => {
    const input = inputs.at(index);
    expect(input.prop('value')).to.equal(value);
  });
};

const assertHidden = (multiValuesInput, index) => {
  expect(multiValuesInput.find('li').at(index).is('.hidden')).to.equal(true);
};

describe('Settings :: Inputs :: MultiValueInput', () => {
  it('should render one value', () => {
    const setting = { definition, values: ['foo'] };
    const multiValueInput = shallow(<MultiValueInput setting={setting} onChange={sinon.stub()}/>);
    const stringInputs = multiValueInput.find(InputForString);
    expect(stringInputs).to.have.length(1);
    assertValues(stringInputs, ['foo']);
  });

  it('should render several values', () => {
    const setting = { definition, values: ['foo', 'bar', 'baz'] };
    const multiValueInput = shallow(<MultiValueInput setting={setting} onChange={sinon.stub()}/>);
    const stringInputs = multiValueInput.find(InputForString);
    expect(stringInputs).to.have.length(3);
    assertValues(stringInputs, ['foo', 'bar', 'baz']);
  });

  it('should add new value', () => {
    const setting = { definition, values: ['foo'] };
    const multiValueInput = shallow(<MultiValueInput setting={setting} onChange={sinon.stub()}/>);
    const stringInputsBefore = multiValueInput.find(InputForString);
    expect(stringInputsBefore).to.have.length(1);

    click(multiValueInput.find('.js-add-value'));

    const stringInputsAfter = multiValueInput.find(InputForString);
    expect(stringInputsAfter).to.have.length(2);
    assertValues(stringInputsAfter, ['foo', '']);
  });

  it('should add first value', () => {
    const setting = { definition, values: [] };
    const multiValueInput = shallow(<MultiValueInput setting={setting} onChange={sinon.stub()}/>);
    const stringInputsBefore = multiValueInput.find(InputForString);
    expect(stringInputsBefore).to.have.length(0);

    click(multiValueInput.find('.js-add-value'));

    const stringInputsAfter = multiValueInput.find(InputForString);
    expect(stringInputsAfter).to.have.length(1);
    assertValues(stringInputsAfter, ['']);
  });

  it('should remove value', () => {
    const setting = { definition, values: ['foo', 'bar'] };
    const multiValueInput = shallow(<MultiValueInput setting={setting} onChange={sinon.stub()}/>);
    const stringInputsBefore = multiValueInput.find(InputForString);
    expect(stringInputsBefore).to.have.length(2);

    click(multiValueInput.find('.js-remove-value').at(0));
    assertHidden(multiValueInput, 0);
  });

  it('should remove value in the middle', () => {
    const setting = { definition, values: ['foo', 'bar', 'baz'] };
    const multiValueInput = shallow(<MultiValueInput setting={setting} onChange={sinon.stub()}/>);
    const stringInputsBefore = multiValueInput.find(InputForString);
    expect(stringInputsBefore).to.have.length(3);

    click(multiValueInput.find('.js-remove-value').at(1));
    assertHidden(multiValueInput, 1);
  });

  it('should remove last existing value', () => {
    const setting = { definition, values: ['foo'] };
    const multiValueInput = shallow(<MultiValueInput setting={setting} onChange={sinon.stub()}/>);
    const stringInputsBefore = multiValueInput.find(InputForString);
    expect(stringInputsBefore).to.have.length(1);

    click(multiValueInput.find('.js-remove-value').at(0));
    assertHidden(multiValueInput, 0);
  });

  it('should change existing value', done => {
    const setting = { definition, values: ['foo', 'bar', 'baz'] };
    const onChange = sinon.spy();
    const multiValueInput = mount(<MultiValueInput setting={setting} onChange={onChange}/>);
    const stringInputs = multiValueInput.find(InputForString);

    change(stringInputs.at(1).find('input'), 'qux');

    setTimeout(() => {
      expect(onChange.called).to.equal(true);
      expect(onChange.lastCall.args).to.deep.equal([undefined, ['foo', 'qux', 'baz']]);
      done();
    }, DEBOUNCE_WAIT + 100);
  });

  it('should change existing value', done => {
    const setting = { definition, values: ['foo', 'bar', 'baz'] };
    const onChange = sinon.spy();
    const multiValueInput = mount(<MultiValueInput setting={setting} onChange={onChange}/>);

    change(multiValueInput.find(InputForString).at(1).find('input'), 'qux');

    setTimeout(() => {
      expect(onChange.called).to.equal(true);
      expect(onChange.lastCall.args).to.deep.equal([undefined, ['foo', 'qux', 'baz']]);
      done();
    }, DEBOUNCE_WAIT + 100);
  });

  it('should add and save new value', done => {
    const setting = { definition, values: ['foo'] };
    const onChange = sinon.spy();
    const multiValueInput = mount(<MultiValueInput setting={setting} onChange={onChange}/>);

    click(multiValueInput.find('.js-add-value'));
    change(multiValueInput.find(InputForString).at(1).find('input'), 'bar');

    setTimeout(() => {
      expect(onChange.called).to.equal(true);
      expect(onChange.lastCall.args).to.deep.equal([undefined, ['foo', 'bar']]);
      done();
    }, DEBOUNCE_WAIT + 100);
  });

  it('should remove and save', done => {
    const setting = { definition, values: ['foo', 'bar'] };
    const onChange = sinon.spy();
    const multiValueInput = mount(<MultiValueInput setting={setting} onChange={onChange}/>);

    click(multiValueInput.find('.js-remove-value').at(1));

    setTimeout(() => {
      expect(onChange.called).to.equal(true);
      expect(onChange.lastCall.args).to.deep.equal([undefined, ['foo']]);
      done();
    }, DEBOUNCE_WAIT + 100);
  });
});
