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
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';
import chai, { expect } from 'chai';
import sinon from 'sinon';
import sinonChai from 'sinon-chai';

import Stats from '../components/Stats';
import Search from '../components/Search';
import { STATUSES, CURRENTS, DEBOUNCE_DELAY, DEFAULT_FILTERS } from '../constants';
import { formatDuration } from '../utils';

chai.use(sinonChai);

describe('Background Tasks', function () {
  describe('Constants', () => {
    it('should have STATUSES', () => {
      expect(STATUSES).to.be.a('object');
      expect(Object.keys(STATUSES).length).to.equal(7);
    });

    it('should have CURRENTS', () => {
      expect(CURRENTS).to.be.a('object');
      expect(Object.keys(CURRENTS).length).to.equal(2);
    });
  });

  describe('Search', () => {
    const defaultProps = {
      ...DEFAULT_FILTERS,
      loading: false,
      types: [],
      onFilterUpdate: () => true,
      onReload: () => true
    };

    it('should render search form', () => {
      const component = TestUtils.renderIntoDocument(
          <Search {...defaultProps}/>
      );
      const searchBox = TestUtils.scryRenderedDOMComponentsWithClass(component, 'js-search');
      expect(searchBox).to.have.length(1);
    });

    it('should not render search form', () => {
      const component = TestUtils.renderIntoDocument(
          <Search {...defaultProps} component={{ id: 'ABCD' }}/>
      );
      const searchBox = TestUtils.scryRenderedDOMComponentsWithClass(component, 'js-search');
      expect(searchBox).to.be.empty;
    });

    it('should search', (done) => {
      const searchSpy = sinon.spy();
      const component = TestUtils.renderIntoDocument(
          <Search {...defaultProps} onFilterUpdate={searchSpy}/>);
      const searchInput = ReactDOM.findDOMNode(
          TestUtils.findRenderedDOMComponentWithClass(component, 'js-search'));
      searchInput.value = 'some search query';
      TestUtils.Simulate.change(searchInput);
      setTimeout(() => {
        expect(searchSpy).to.have.been.calledWith({ query: 'some search query' });
        done();
      }, DEBOUNCE_DELAY);
    });

    it('should reload', () => {
      const reloadSpy = sinon.spy();
      const component = TestUtils.renderIntoDocument(
          <Search {...defaultProps} onReload={reloadSpy}/>
      );
      const reloadButton = component.refs.reloadButton;
      expect(reloadSpy).to.not.have.been.called;
      TestUtils.Simulate.click(reloadButton);
      expect(reloadSpy).to.have.been.called;
    });
  });

  describe('Stats', () => {
    describe('Pending', () => {
      it('should show zero pending', () => {
        const result = TestUtils.renderIntoDocument(<Stats pendingCount={0}/>);
        const pendingCounter = result.refs.pendingCount;
        expect(pendingCounter.textContent).to.contain('0');
      });

      it('should show 5 pending', () => {
        const result = TestUtils.renderIntoDocument(<Stats pendingCount={5}/>);
        const pendingCounter = result.refs.pendingCount;
        expect(pendingCounter.textContent).to.contain('5');
      });

      it('should not show cancel pending button', () => {
        const result = TestUtils.renderIntoDocument(<Stats pendingCount={0}/>);
        const cancelPending = result.refs.cancelPending;
        expect(cancelPending).to.not.be.ok;
      });

      it('should show cancel pending button', () => {
        const result = TestUtils.renderIntoDocument(<Stats pendingCount={5}/>);
        const cancelPending = result.refs.cancelPending;
        expect(cancelPending).to.be.ok;
      });

      it('should trigger cancelling pending', () => {
        const spy = sinon.spy();
        const result = TestUtils.renderIntoDocument(<Stats pendingCount={5} onCancelAllPending={spy}/>);
        const cancelPending = result.refs.cancelPending;
        expect(spy).to.not.have.been.called;
        TestUtils.Simulate.click(cancelPending);
        expect(spy).to.have.been.called;
      });
    });

    describe('Failures', () => {
      it('should show zero failures', () => {
        const result = TestUtils.renderIntoDocument(<Stats failingCount={0}/>);
        const failureCounter = result.refs.failureCount;
        expect(failureCounter.textContent).to.contain('0');
      });

      it('should show 5 failures', () => {
        const result = TestUtils.renderIntoDocument(<Stats failingCount={5}/>);
        const failureCounter = result.refs.failureCount;
        expect(failureCounter.textContent).to.contain('5');
      });

      it('should not show link to failures', () => {
        const result = TestUtils.renderIntoDocument(<Stats failingCount={0}/>);
        const failureCounter = result.refs.failureCount;
        expect(failureCounter.tagName.toLowerCase()).to.not.equal('a');
      });

      it('should show link to failures', () => {
        const result = TestUtils.renderIntoDocument(<Stats failingCount={5}/>);
        const failureCounter = result.refs.failureCount;
        expect(failureCounter.tagName.toLowerCase()).to.equal('a');
      });

      it('should trigger filtering failures', () => {
        const spy = sinon.spy();
        const result = TestUtils.renderIntoDocument(<Stats failingCount={5} onShowFailing={spy}/>);
        const failureCounter = result.refs.failureCount;
        expect(spy).to.not.have.been.called;
        TestUtils.Simulate.click(failureCounter);
        expect(spy).to.have.been.called;
      });
    });
  });

  describe('Helpers', () => {
    describe('#formatDuration()', () => {
      it('should format 173ms', () => {
        expect(formatDuration(173)).to.equal('173ms');
      });

      it('should format 999ms', () => {
        expect(formatDuration(999)).to.equal('999ms');
      });

      it('should format 1s', () => {
        expect(formatDuration(1000)).to.equal('1s');
      });

      it('should format 1s', () => {
        expect(formatDuration(1001)).to.equal('1s');
      });

      it('should format 2s', () => {
        expect(formatDuration(1501)).to.equal('2s');
      });

      it('should format 59s', () => {
        expect(formatDuration(59000)).to.equal('59s');
      });

      it('should format 1min', () => {
        expect(formatDuration(60000)).to.equal('1min');
      });

      it('should format 1min', () => {
        expect(formatDuration(62757)).to.equal('1min');
      });

      it('should format 4min', () => {
        expect(formatDuration(224567)).to.equal('4min');
      });

      it('should format 80min', () => {
        expect(formatDuration(80 * 60 * 1000)).to.equal('80min');
      });
    });
  });
});
