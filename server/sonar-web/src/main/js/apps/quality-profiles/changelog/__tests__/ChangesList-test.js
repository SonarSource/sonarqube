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
import { expect } from 'chai';
import { shallow } from 'enzyme';
import React from 'react';
import ChangesList from '../ChangesList';
import SeverityChange from '../SeverityChange';
import ParameterChange from '../ParameterChange';

describe('Quality Profiles :: ChangesList', () => {
  it('should render changes', () => {
    const changes = { severity: 'BLOCKER', foo: 'bar' };
    const output = shallow(
        <ChangesList changes={changes}/>
    );
    expect(output.find('li')).to.have.length(2);
  });

  it('should render severity change', () => {
    const changes = { severity: 'BLOCKER' };
    const output = shallow(
        <ChangesList changes={changes}/>
    ).find(SeverityChange);
    expect(output).to.have.length(1);
    expect(output.prop('severity')).to.equal('BLOCKER');
  });

  it('should render parameter change', () => {
    const changes = { foo: 'bar' };
    const output = shallow(
        <ChangesList changes={changes}/>
    ).find(ParameterChange);
    expect(output).to.have.length(1);
    expect(output.prop('name')).to.equal('foo');
    expect(output.prop('value')).to.equal('bar');
  });
});
