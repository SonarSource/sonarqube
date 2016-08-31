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
import Input from '../Input';
import InputForString from '../InputForString';
import InputForText from '../InputForText';
import InputForPassword from '../InputForPassword';
import InputForBoolean from '../InputForBoolean';
import InputForNumber from '../InputForNumber';
import InputForSingleSelectList from '../InputForSingleSelectList';
import * as constants from '../../../constants';

const exampleSetting = type => ({
  definition: { key: 'example', type },
  value: 'sample'
});

describe('Settings :: Inputs :: Input', () => {
  it('should render InputForString by default', () => {
    const setting = exampleSetting('UNKNOWN');
    const onChange = sinon.spy();
    const input = shallow(<Input setting={setting} value="foo" onChange={onChange}/>).find(InputForString);
    expect(input).to.have.length(1);
    expect(input.prop('name')).to.be.a('string');
    expect(input.prop('value')).to.equal('foo');
    expect(input.prop('onChange')).to.equal(onChange);
  });

  it('should render InputForString', () => {
    const setting = exampleSetting(constants.TYPE_STRING);
    const onChange = sinon.spy();
    const input = shallow(<Input setting={setting} value="foo" onChange={onChange}/>).find(InputForString);
    expect(input).to.have.length(1);
    expect(input.prop('name')).to.be.a('string');
    expect(input.prop('value')).to.equal('foo');
    expect(input.prop('onChange')).to.equal(onChange);
  });

  it('should render InputForText', () => {
    const setting = exampleSetting(constants.TYPE_TEXT);
    const onChange = sinon.spy();
    const input = shallow(<Input setting={setting} value="foo" onChange={onChange}/>).find(InputForText);
    expect(input).to.have.length(1);
    expect(input.prop('name')).to.be.a('string');
    expect(input.prop('value')).to.equal('foo');
    expect(input.prop('onChange')).to.equal(onChange);
  });

  it('should render InputForPassword', () => {
    const setting = exampleSetting(constants.TYPE_PASSWORD);
    const onChange = sinon.spy();
    const input = shallow(<Input setting={setting} value="foo" onChange={onChange}/>).find(InputForPassword);
    expect(input).to.have.length(1);
    expect(input.prop('name')).to.be.a('string');
    expect(input.prop('value')).to.equal('foo');
    expect(input.prop('onChange')).to.equal(onChange);
  });

  it('should render InputForBoolean', () => {
    const setting = { ...exampleSetting(constants.TYPE_BOOLEAN), value: true };
    const onChange = sinon.spy();
    const input = shallow(<Input setting={setting} value={true} onChange={onChange}/>).find(InputForBoolean);
    expect(input).to.have.length(1);
    expect(input.prop('name')).to.be.a('string');
    expect(input.prop('value')).to.equal(true);
    expect(input.prop('onChange')).to.equal(onChange);
  });

  it('should render InputForNumber', () => {
    const setting = exampleSetting(constants.TYPE_INTEGER);
    const onChange = sinon.spy();
    const input = shallow(<Input setting={setting} value={17} onChange={onChange}/>).find(InputForNumber);
    expect(input).to.have.length(1);
    expect(input.prop('name')).to.be.a('string');
    expect(input.prop('value')).to.equal(17);
    expect(input.prop('onChange')).to.equal(onChange);
  });

  it('should render InputForSingleSelectList', () => {
    const options = ['foo', 'bar', 'baz'];
    const setting = {
      definition: { key: 'example', type: constants.TYPE_SINGLE_SELECT_LIST, options },
      value: 'bar'
    };
    const onChange = sinon.spy();
    const input = shallow(<Input setting={setting} value="bar" onChange={onChange}/>).find(InputForSingleSelectList);
    expect(input).to.have.length(1);
    expect(input.prop('name')).to.be.a('string');
    expect(input.prop('value')).to.equal('bar');
    expect(input.prop('options')).to.equal(options);
    expect(input.prop('onChange')).to.equal(onChange);
  });
});
