/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import Stats from '../components/Stats';
import Search from '../components/Search';
import { STATUSES, CURRENTS, DEBOUNCE_DELAY, DEFAULT_FILTERS } from '../constants';
import { formatDuration } from '../utils';
import { click } from '../../../helpers/testUtils';

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
  const defaultProps: Search['props'] = {
    ...DEFAULT_FILTERS,
    loading: false,
    types: [],
    onFilterUpdate: () => true,
    onReload: () => true,
    maxExecutedAt: undefined,
    minSubmittedAt: undefined
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
    searchInput.prop<Function>('onChange')('some search query');
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
        <Stats onCancelAllPending={stub} onShowFailing={stub} pendingCount={0} />
      );
      expect(result.find('.js-pending-count').text()).toContain('0');
    });

    it('should show 5 pending', () => {
      const result = shallow(
        <Stats onCancelAllPending={stub} onShowFailing={stub} pendingCount={5} />
      );
      expect(result.find('.js-pending-count').text()).toContain('5');
    });

    it('should not show cancel pending button', () => {
      const result = shallow(
        <Stats onCancelAllPending={stub} onShowFailing={stub} pendingCount={0} />
      );
      expect(result.find('[data-test="cancel-pending"]').length).toBe(0);
    });

    it('should show cancel pending button', () => {
      const result = shallow(
        <Stats
          isSystemAdmin={true}
          onCancelAllPending={stub}
          onShowFailing={stub}
          pendingCount={5}
        />
      );
      expect(result.find('[data-test="cancel-pending"]').length).toBe(1);
    });

    it('should trigger cancelling pending', () => {
      const spy = jest.fn();
      const result = shallow(
        <Stats
          isSystemAdmin={true}
          onCancelAllPending={spy}
          onShowFailing={stub}
          pendingCount={5}
        />
      );
      expect(spy).not.toBeCalled();
      result.find('[data-test="cancel-pending"]').prop<Function>('onConfirm')();
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

    it('should format 1s 0ms', () => {
      expect(formatDuration(1000)).toBe('1.0s');
    });

    it('should format 1s 1ms', () => {
      expect(formatDuration(1001)).toBe('1.1s');
    });

    it('should format 1s 501ms', () => {
      expect(formatDuration(1501)).toBe('1.501s');
    });

    it('should format 59s', () => {
      expect(formatDuration(59000)).toBe('59s');
    });

    it('should format 1min 0s', () => {
      expect(formatDuration(60000)).toBe('1min 0s');
    });

    it('should format 1min 2s', () => {
      expect(formatDuration(62757)).toBe('1min 2s');
    });

    it('should format 3min 44s', () => {
      expect(formatDuration(224567)).toBe('3min 44s');
    });

    it('should format 1h 20m', () => {
      expect(formatDuration(80 * 60 * 1000)).toBe('1h 20min');
    });
  });
});
