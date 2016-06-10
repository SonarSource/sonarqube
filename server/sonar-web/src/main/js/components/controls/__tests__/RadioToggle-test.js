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
import chai, { expect } from 'chai';
import { shallow } from 'enzyme';
import sinon from 'sinon';
import sinonChai from 'sinon-chai';
import React from 'react';
import RadioToggle from '../RadioToggle';

chai.use(sinonChai);

function getSample (props) {
  const options = [
    { value: 'one', label: 'first' },
    { value: 'two', label: 'second' }
  ];
  return (
      <RadioToggle
          options={options}
          name="sample"
          onCheck={() => true}
          {...props}/>
  );
}

function change (element, value) {
  return element.simulate('change', { currentTarget: { value } });
}

describe('Components :: Controls :: RadioToggle', () => {
  it('should render', () => {
    const radioToggle = shallow(getSample());
    expect(radioToggle.find('input[type="radio"]')).to.have.length(2);
    expect(radioToggle.find('label')).to.have.length(2);
  });

  it('should call onCheck', () => {
    const onCheck = sinon.spy();
    const radioToggle = shallow(getSample({ onCheck }));
    change(radioToggle.find('input[value="two"]'), 'two');
    expect(onCheck).to.have.been.calledWith('two');
  });
});
