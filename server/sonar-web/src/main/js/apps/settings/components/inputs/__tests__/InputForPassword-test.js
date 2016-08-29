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
import InputForPassword from '../InputForPassword';
import { click, submit } from '../../../../../../../../tests/utils';

describe('Settings :: Inputs :: InputForPassword', () => {
  it('should render lock icon, but no form', () => {
    const onChange = sinon.spy();
    const input = shallow(
        <InputForPassword
            name="foo"
            value="bar"
            isDefault={false}
            onChange={onChange}/>
    );
    expect(input.find('.icon-lock')).to.have.length(1);
    expect(input.find('form')).to.have.length(0);
  });

  it('should open form', () => {
    const onChange = sinon.spy();
    const input = shallow(
        <InputForPassword
            name="foo"
            value="bar"
            isDefault={false}
            onChange={onChange}/>
    );
    const button = input.find('button');
    expect(button).to.have.length(1);

    click(button);
    expect(input.find('form')).to.have.length(1);
  });

  it('should close form', () => {
    const onChange = sinon.spy();
    const input = shallow(
        <InputForPassword
            name="foo"
            value="bar"
            isDefault={false}
            onChange={onChange}/>
    );
    const button = input.find('button');
    expect(button).to.have.length(1);

    click(button);
    expect(input.find('form')).to.have.length(1);

    click(input.find('form').find('a'));
    expect(input.find('form')).to.have.length(0);
  });

  it('should set value', () => {
    const onChange = sinon.stub().returns(Promise.resolve());
    const input = mount(
        <InputForPassword
            name="foo"
            value="bar"
            isDefault={false}
            onChange={onChange}/>
    );
    const button = input.find('button');
    expect(button).to.have.length(1);

    click(button);
    const form = input.find('form');
    expect(form).to.have.length(1);

    input.ref('input').value = 'secret';
    submit(form);

    expect(onChange.called).to.equal(true);
  });
});
