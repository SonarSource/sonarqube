/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import { shallow } from 'enzyme';
import Stats from '../components/Stats';
import Search from '../components/Search';
import { STATUSES, CURRENTS, DEBOUNCE_DELAY, DEFAULT_FILTERS } from '../constants';
import { formatDuration } from '../utils';
import { change, click } from '../../../helpers/testUtils';

const stub = jest.fn();

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
    const component = shallow(<Search {...defaultProps} />);
    expect(component.find('SearchBox').exists()).toBeTruthy();
  });

  it('should not render search form', () => {
    const component = shallow(<Search {...defaultProps} component={{ id: 'ABCD' }} />);
    expect(component.find('SearchBox').exists()).toBeFalsy();
  });

  it('should search', done => {
    const searchSpy = jest.fn();
    const component = shallow(<Search {...defaultProps} onFilterUpdate={searchSpy} />);
    const searchInput = component.find('SearchBox');
    searchInput.prop('onChange')('some search query');
    setTimeout(() => {
      expect(searchSpy).toBeCalledWith({ query: 'some search query' });
      done();
    }, DEBOUNCE_DELAY);
  });

  it('should reload', () => {
    const reloadSpy = jest.fn();
    const component = shallow(<Search {...defaultProps} onReload={reloadSpy} />);
    const reloadButton = component.find('.js-reload');
    expect(reloadSpy).not.toBeCalled();
    click(reloadButton);
    expect(reloadSpy).toBeCalled();
  });
});

describe('Stats', () => {
  describe('Pending', () => {
    it('should show zero pending', () => {
      const result = shallow(
        <Stats pendingCount={0} onCancelAllPending={stub} onShowFailing={stub} />
      );
      expect(result.find('.js-pending-count').text()).toContain('0');
    });

    it('should show 5 pending', () => {
      const result = shallow(
        <Stats pendingCount={5} onCancelAllPending={stub} onShowFailing={stub} />
      );
      expect(result.find('.js-pending-count').text()).toContain('5');
    });

    it('should not show cancel pending button', () => {
      const result = shallow(
        <Stats pendingCount={0} onCancelAllPending={stub} onShowFailing={stub} />
      );
      expect(result.find('.js-cancel-pending').length).toBe(0);
    });

    it('should show cancel pending button', () => {
      const result = shallow(
        <Stats
          isSystemAdmin={true}
          pendingCount={5}
          onCancelAllPending={stub}
          onShowFailing={stub}
        />
      );
      expect(result.find('.js-cancel-pending').length).toBe(1);
    });

    it('should trigger cancelling pending', () => {
      const spy = jest.fn();
      const result = shallow(
        <Stats
          isSystemAdmin={true}
          pendingCount={5}
          onCancelAllPending={spy}
          onShowFailing={stub}
        />
      );
      expect(spy).not.toBeCalled();
      click(result.find('.js-cancel-pending'));
      expect(spy).toBeCalled();
    });
  });

  describe('Failures', () => {
    it('should show zero failures', () => {
      const result = shallow(
        <Stats failingCount={0} onCancelAllPending={stub} onShowFailing={stub} />
      );
      expect(result.find('.js-failures-count').text()).toContain('0');
    });

    it('should show 5 failures', () => {
      const result = shallow(
        <Stats failingCount={5} onCancelAllPending={stub} onShowFailing={stub} />
      );
      expect(result.find('.js-failures-count').text()).toContain('5');
    });

    it('should not show link to failures', () => {
      const result = shallow(
        <Stats failingCount={0} onCancelAllPending={stub} onShowFailing={stub} />
      );
      expect(result.find('.js-failures-count').is('a')).toBeFalsy();
    });

    it('should show link to failures', () => {
      const result = shallow(
        <Stats failingCount={5} onCancelAllPending={stub} onShowFailing={stub} />
      );
      expect(result.find('.js-failures-count').is('a')).toBeTruthy();
    });

    it('should trigger filtering failures', () => {
      const spy = jest.fn();
      const result = shallow(
        <Stats failingCount={5} onCancelAllPending={stub} onShowFailing={spy} />
      );
      expect(spy).not.toBeCalled();
      click(result.find('.js-failures-count'));
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
