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
import Stats from '../components/Stats';
import Search from '../components/Search';
import { STATUSES, CURRENTS, DEBOUNCE_DELAY, DEFAULT_FILTERS } from '../constants';
import { formatDuration } from '../utils';

const stub = jest.fn();

describe('Background Tasks', function () {
  describe('Constants', () => {
    it('should have STATUSES', () => {
      expect(Object.keys(STATUSES).length).toBe(7);
    });

    it('should have CURRENTS', () => {
      expect(Object.keys(CURRENTS).length).toBe(2);
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
      expect(searchBox.length).toBe(1);
    });

    it('should not render search form', () => {
      const component = TestUtils.renderIntoDocument(
          <Search {...defaultProps} component={{ id: 'ABCD' }}/>
      );
      const searchBox = TestUtils.scryRenderedDOMComponentsWithClass(component, 'js-search');
      expect(searchBox.length).toBe(0);
    });

    it('should search', (done) => {
      const searchSpy = jest.fn();
      const component = TestUtils.renderIntoDocument(
          <Search {...defaultProps} onFilterUpdate={searchSpy}/>);
      const searchInput = ReactDOM.findDOMNode(
          TestUtils.findRenderedDOMComponentWithClass(component, 'js-search'));
      searchInput.value = 'some search query';
      TestUtils.Simulate.change(searchInput);
      setTimeout(() => {
        expect(searchSpy).toBeCalledWith({ query: 'some search query' });
        done();
      }, DEBOUNCE_DELAY);
    });

    it('should reload', () => {
      const reloadSpy = jest.fn();
      const component = TestUtils.renderIntoDocument(
          <Search {...defaultProps} onReload={reloadSpy}/>
      );
      const reloadButton = component.refs.reloadButton;
      expect(reloadSpy).not.toBeCalled();
      TestUtils.Simulate.click(reloadButton);
      expect(reloadSpy).toBeCalled();
    });
  });

  describe('Stats', () => {
    describe('Pending', () => {
      it('should show zero pending', () => {
        const result = TestUtils.renderIntoDocument(
            <Stats pendingCount={0} onCancelAllPending={stub} onShowFailing={stub}/>);
        const pendingCounter = result.refs.pendingCount;
        expect(pendingCounter.textContent).toContain('0');
      });

      it('should show 5 pending', () => {
        const result = TestUtils.renderIntoDocument(
            <Stats pendingCount={5} onCancelAllPending={stub} onShowFailing={stub}/>);
        const pendingCounter = result.refs.pendingCount;
        expect(pendingCounter.textContent).toContain('5');
      });

      it('should not show cancel pending button', () => {
        const result = TestUtils.renderIntoDocument(
            <Stats pendingCount={0} onCancelAllPending={stub} onShowFailing={stub}/>);
        expect(result.refs.cancelPending).not.toBeTruthy();
      });

      it('should show cancel pending button', () => {
        const result = TestUtils.renderIntoDocument(
            <Stats pendingCount={5} onCancelAllPending={stub} onShowFailing={stub}/>);
        expect(result.refs.cancelPending).toBeTruthy();
      });

      it('should trigger cancelling pending', () => {
        const spy = jest.fn();
        const result = TestUtils.renderIntoDocument(
            <Stats pendingCount={5} onCancelAllPending={spy} onShowFailing={stub}/>);
        const cancelPending = result.refs.cancelPending;
        expect(spy).not.toBeCalled();
        TestUtils.Simulate.click(cancelPending);
        expect(spy).toBeCalled();
      });
    });

    describe('Failures', () => {
      it('should show zero failures', () => {
        const result = TestUtils.renderIntoDocument(
            <Stats failingCount={0} onCancelAllPending={stub} onShowFailing={stub}/>);
        const failureCounter = result.refs.failureCount;
        expect(failureCounter.textContent).toContain('0');
      });

      it('should show 5 failures', () => {
        const result = TestUtils.renderIntoDocument(
            <Stats failingCount={5} onCancelAllPending={stub} onShowFailing={stub}/>);
        const failureCounter = result.refs.failureCount;
        expect(failureCounter.textContent).toContain('5');
      });

      it('should not show link to failures', () => {
        const result = TestUtils.renderIntoDocument(
            <Stats failingCount={0} onCancelAllPending={stub} onShowFailing={stub}/>);
        const failureCounter = result.refs.failureCount;
        expect(failureCounter.tagName.toLowerCase()).not.toBe('a');
      });

      it('should show link to failures', () => {
        const result = TestUtils.renderIntoDocument(
            <Stats failingCount={5} onCancelAllPending={stub} onShowFailing={stub}/>);
        const failureCounter = result.refs.failureCount;
        expect(failureCounter.tagName.toLowerCase()).toBe('a');
      });

      it('should trigger filtering failures', () => {
        const spy = jest.fn();
        const result = TestUtils.renderIntoDocument(
            <Stats failingCount={5} onCancelAllPending={stub} onShowFailing={spy}/>);
        const failureCounter = result.refs.failureCount;
        expect(spy).not.toBeCalled();
        TestUtils.Simulate.click(failureCounter);
        expect(spy).toBeCalled();
      });
    });
  });

  describe('Helpers', () => {
    describe('#formatDuration()', () => {
      it('should format 173ms', () => {
        expect(formatDuration(173)).toBe('173ms');
      });

      it('should format 999ms', () => {
        expect(formatDuration(999)).toBe('999ms');
      });

      it('should format 1s', () => {
        expect(formatDuration(1000)).toBe('1s');
      });

      it('should format 1s', () => {
        expect(formatDuration(1001)).toBe('1s');
      });

      it('should format 2s', () => {
        expect(formatDuration(1501)).toBe('2s');
      });

      it('should format 59s', () => {
        expect(formatDuration(59000)).toBe('59s');
      });

      it('should format 1min', () => {
        expect(formatDuration(60000)).toBe('1min');
      });

      it('should format 1min', () => {
        expect(formatDuration(62757)).toBe('1min');
      });

      it('should format 4min', () => {
        expect(formatDuration(224567)).toBe('4min');
      });

      it('should format 80min', () => {
        expect(formatDuration(80 * 60 * 1000)).toBe('80min');
      });
    });
  });
});
