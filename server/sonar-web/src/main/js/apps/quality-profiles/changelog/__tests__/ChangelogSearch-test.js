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
import sinon from 'sinon';
import React from 'react';
import ChangelogSearch from '../ChangelogSearch';
import DateInput from '../../../../components/controls/DateInput';

function click (element) {
  return element.simulate('click', {
    target: { blur () {} },
    preventDefault () {}
  });
}

describe('Quality Profiles :: ChangelogSearch', () => {
  it('should render DateInput', () => {
    const onFromDateChange = sinon.spy();
    const onToDateChange = sinon.spy();
    const output = shallow(
        <ChangelogSearch
            fromDate="2016-01-01"
            toDate="2016-05-05"
            onFromDateChange={onFromDateChange}
            onToDateChange={onToDateChange}
            onReset={sinon.spy()}/>
    );
    const dateInputs = output.find(DateInput);
    expect(dateInputs).to.have.length(2);
    expect(dateInputs.at(0).prop('value')).to.equal('2016-01-01');
    expect(dateInputs.at(0).prop('onChange')).to.equal(onFromDateChange);
    expect(dateInputs.at(1).prop('value')).to.equal('2016-05-05');
    expect(dateInputs.at(1).prop('onChange')).to.equal(onToDateChange);
  });

  it('should reset', () => {
    const onReset = sinon.spy();
    const output = shallow(
        <ChangelogSearch
            fromDate="2016-01-01"
            toDate="2016-05-05"
            onFromDateChange={sinon.spy()}
            onToDateChange={sinon.spy()}
            onReset={onReset}/>
    );
    expect(onReset.called).to.equal(false);
    click(output.find('button'));
    expect(onReset.called).to.equal(true);
  });
});
