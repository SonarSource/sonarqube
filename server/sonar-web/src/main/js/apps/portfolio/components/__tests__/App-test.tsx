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
/* eslint-disable import/first, import/order */
jest.mock('../../../../api/measures', () => ({
  getMeasures: jest.fn(() => Promise.resolve([]))
}));

jest.mock('../../../../api/components', () => ({
  getChildren: jest.fn(() => Promise.resolve({ components: [], paging: { total: 0 } }))
}));

// mock Activity to not deal with localstorage
jest.mock('../Activity', () => ({
  // eslint-disable-next-line
  default: function Activity() {
    return null;
  }
}));

jest.mock('../Report', () => ({
  // eslint-disable-next-line
  default: function Report() {
    return null;
  }
}));

import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { App } from '../App';

const getMeasures = require('../../../../api/measures').getMeasures as jest.Mock<any>;
const getChildren = require('../../../../api/components').getChildren as jest.Mock<any>;

const component = { key: 'foo', name: 'Foo' };

it('renders', () => {
  const wrapper = shallow(<App component={component} fetchMetrics={jest.fn()} metrics={{}} />);
  wrapper.setState({
    loading: false,
    measures: { ncloc: '173', reliability_rating: '1' },
    subComponents: [],
    totalSubComponents: 0
  });
  expect(wrapper).toMatchSnapshot();
});

it('renders when portfolio is empty', () => {
  const wrapper = shallow(<App component={component} fetchMetrics={jest.fn()} metrics={{}} />);
  wrapper.setState({ loading: false, measures: { reliability_rating: '1' } });
  expect(wrapper).toMatchSnapshot();
});

it('renders when portfolio is not computed', () => {
  const wrapper = shallow(<App component={component} fetchMetrics={jest.fn()} metrics={{}} />);
  wrapper.setState({ loading: false, measures: { ncloc: '173' } });
  expect(wrapper).toMatchSnapshot();
});

it('fetches measures and children components', () => {
  getMeasures.mockClear();
  getChildren.mockClear();
  mount(<App component={component} fetchMetrics={jest.fn()} metrics={{}} />);
  expect(getMeasures).toBeCalledWith('foo', [
    'projects',
    'ncloc',
    'ncloc_language_distribution',
    'releasability_rating',
    'releasability_effort',
    'sqale_rating',
    'maintainability_rating_effort',
    'reliability_rating',
    'reliability_rating_effort',
    'security_rating',
    'security_rating_effort',
    'last_change_on_releasability_rating',
    'last_change_on_maintainability_rating',
    'last_change_on_security_rating',
    'last_change_on_reliability_rating'
  ]);
  expect(getChildren).toBeCalledWith(
    'foo',
    [
      'ncloc',
      'releasability_rating',
      'security_rating',
      'reliability_rating',
      'sqale_rating',
      'alert_status'
    ],
    { ps: 20, s: 'qualifier' }
  );
});
