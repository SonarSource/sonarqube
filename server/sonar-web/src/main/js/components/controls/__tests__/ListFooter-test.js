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
import ListFooter from '../ListFooter';

chai.use(sinonChai);

function click (element) {
  return element.simulate('click', {
    target: { blur () {} },
    preventDefault () {}
  });
}

describe('Components :: Controls :: ListFooter', () => {
  it('should render "3 of 5 shown"', () => {
    const listFooter = shallow(
        <ListFooter count={3} total={5}/>
    );
    expect(listFooter.text()).to.contain('x_of_y_shown.3.5');
  });

  it('should not render "show more"', () => {
    const listFooter = shallow(
        <ListFooter count={3} total={5}/>
    );
    expect(listFooter.find('a')).to.have.length(0);
  });

  it('should "show more"', () => {
    const loadMore = sinon.spy();
    const listFooter = shallow(
        <ListFooter count={3} total={5} loadMore={loadMore}/>
    );
    const link = listFooter.find('a');
    expect(link).to.have.length(1);
    click(link);
    expect(loadMore).to.have.been.called;
  });
});
