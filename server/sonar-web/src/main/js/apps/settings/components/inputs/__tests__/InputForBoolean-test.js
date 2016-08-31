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
import InputForBoolean from '../InputForBoolean';
import Toggle from '../../../../../components/controls/Toggle';

describe('Settings :: Inputs :: InputForBoolean', () => {
  it('should render Toggle', () => {
    const onChange = sinon.spy();
    const toggle = shallow(
        <InputForBoolean
            name="foo"
            value={true}
            isDefault={false}
            onChange={onChange}/>
    ).find(Toggle);
    expect(toggle).to.have.length(1);
    expect(toggle.prop('name')).to.equal('foo');
    expect(toggle.prop('value')).to.equal(true);
    expect(toggle.prop('onChange')).to.be.a('function');
  });

  it('should render Toggle without value', () => {
    const onChange = sinon.spy();
    const input = shallow(
        <InputForBoolean
            name="foo"
            isDefault={false}
            onChange={onChange}/>
    );
    const toggle = input.find(Toggle);
    expect(toggle).to.have.length(1);
    expect(toggle.prop('name')).to.equal('foo');
    expect(toggle.prop('value')).to.equal(false);
    expect(toggle.prop('onChange')).to.be.a('function');
    expect(input.find('.note')).to.have.length(1);
  });

  it('should call onChange', () => {
    const onChange = sinon.spy();
    const input = shallow(
        <InputForBoolean
            name="foo"
            value={true}
            isDefault={false}
            onChange={onChange}/>
    );
    const toggle = input.find(Toggle);
    expect(toggle).to.have.length(1);
    expect(toggle.prop('onChange')).to.be.a('function');

    toggle.prop('onChange')(false);

    expect(onChange.called).to.equal(true);
    expect(onChange.lastCall.args).to.deep.equal([undefined, false]);
  });
});
