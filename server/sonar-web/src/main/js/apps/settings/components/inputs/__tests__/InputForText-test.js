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
import InputForText from '../InputForText';
import { change } from '../../../../../../../../tests/utils';

describe('Settings :: Inputs :: InputForText', () => {
  it('should render textarea', () => {
    const onChange = sinon.spy();
    const textarea = shallow(
        <InputForText
            name="foo"
            value="bar"
            isDefault={false}
            onChange={onChange}/>
    ).find('textarea');
    expect(textarea).to.have.length(1);
    expect(textarea.prop('name')).to.equal('foo');
    expect(textarea.prop('value')).to.equal('bar');
    expect(textarea.prop('onChange')).to.be.a('function');
  });

  it('should call onChange', done => {
    const onChange = sinon.spy();
    const textarea = shallow(
        <InputForText
            name="foo"
            value="bar"
            isDefault={false}
            onChange={onChange}/>
    ).find('textarea');
    expect(textarea).to.have.length(1);
    expect(textarea.prop('onChange')).to.be.a('function');

    change(textarea, 'qux');

    setTimeout(() => {
      expect(onChange.called).to.equal(true);
      expect(onChange.lastCall.args).to.deep.equal([undefined, 'qux']);
      done();
    }, 300);
  });
});
