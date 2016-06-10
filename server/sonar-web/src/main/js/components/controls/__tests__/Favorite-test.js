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
import mockery from 'mockery';
import chai, { expect } from 'chai';
import { shallow, mount } from 'enzyme';
import sinon from 'sinon';
import sinonChai from 'sinon-chai';
import React from 'react';

chai.use(sinonChai);

function click (element) {
  return element.simulate('click', {
    target: { blur () {} },
    preventDefault () {}
  });
}

describe('Components :: Controls :: Favorite', () => {
  let addFavoriteStub;
  let removeFavoriteStub;
  let Favorite;
  let FavoriteInner;

  before(() => {
    mockery.enable({
      warnOnReplace: false,
      warnOnUnregistered: false,
      useCleanCache: true
    });

    addFavoriteStub = sinon.stub().returns(Promise.resolve());
    removeFavoriteStub = sinon.stub().returns(Promise.resolve());

    mockery.registerMock('../../api/favorites', {
      addFavorite: addFavoriteStub,
      removeFavorite: removeFavoriteStub
    });

    Favorite = require('../Favorite').default;
    FavoriteInner = require('../Favorite').FavoriteInner;
  });

  after(() => {
    mockery.disable();
  });

  beforeEach(() => {
    addFavoriteStub.reset();
    removeFavoriteStub.reset();
  });

  it('should render FavoriteInner', () => {
    const favorite = shallow(
        <Favorite favorite={true} component="key"/>
    );
    const inner = favorite.find(FavoriteInner);
    expect(inner).to.have.length(1);
    expect(inner.prop('favorite')).to.equal(true);
    expect(inner.prop('onToggle')).to.be.defined;
  });

  it('should remove favorite', () => {
    const favorite = mount(
        <Favorite favorite={true} component="key"/>
    );
    click(favorite.find('a'));
    expect(removeFavoriteStub).to.have.been.calledWith('key');
  });

  it('should add favorite', () => {
    const favorite = mount(
        <Favorite favorite={false} component="key"/>
    );
    click(favorite.find('a'));
    expect(addFavoriteStub).to.have.been.calledWith('key');
  });

  it('should render favorite', () => {
    const favorite = shallow(
        <FavoriteInner favorite={true} onToggle={() => true}/>
    );
    expect(favorite.is('.icon-star-favorite')).to.equal(true);
  });

  it('should render not favorite', () => {
    const favorite = shallow(
        <FavoriteInner favorite={false} onToggle={() => true}/>
    );
    expect(favorite.is('.icon-star-favorite')).to.equal(false);
  });

  it('should call onToggle', () => {
    const onToggle = sinon.spy();
    const favorite = shallow(
        <FavoriteInner favorite={true} onToggle={onToggle}/>
    );
    click(favorite);
    expect(onToggle).to.have.been.called;
  });
});
