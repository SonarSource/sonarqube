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
/* eslint-disable import/first, import/order */
jest.mock('../../../../api/security-reports', () => ({
  getSecurityHotspots: jest.fn(() => {
    const distribution: any = [
      {
        cwe: '477',
        vulnerabilities: 1,
        vulnerabiliyRating: 1,
        toReviewSecurityHotspots: 2,
        openSecurityHotspots: 10,
        wontFixSecurityHotspots: 0
      },
      {
        cwe: '396',
        vulnerabilities: 2,
        vulnerabiliyRating: 2,
        toReviewSecurityHotspots: 2,
        openSecurityHotspots: 10,
        wontFixSecurityHotspots: 0
      }
    ];
    return Promise.resolve({
      categories: [
        {
          activeRules: 1,
          totalRules: 1,
          category: 'a1',
          vulnerabilities: 2,
          vulnerabiliyRating: 5,
          toReviewSecurityHotspots: 2,
          openSecurityHotspots: 10,
          wontFixSecurityHotspots: 0,
          distribution
        },
        {
          activeRules: 1,
          totalRules: 1,
          category: 'a2',
          vulnerabilities: 3,
          vulnerabiliyRating: 3,
          toReviewSecurityHotspots: 8,
          openSecurityHotspots: 100,
          wontFixSecurityHotspots: 10
        },
        {
          activeRules: 0,
          totalRules: 1,
          category: 'a3',
          vulnerabilities: 3,
          vulnerabiliyRating: 3,
          toReviewSecurityHotspots: 8,
          openSecurityHotspots: 100,
          wontFixSecurityHotspots: 10
        }
      ]
    });
  })
}));

import * as React from 'react';
import { shallow } from 'enzyme';
import { App } from '../App';
import { waitAndUpdate } from '../../../../helpers/testUtils';

const getSecurityHotspots = require('../../../../api/security-reports')
  .getSecurityHotspots as jest.Mock<any>;

const component = { key: 'foo', name: 'Foo', qualifier: 'TRK' } as T.Component;
const location = { pathname: 'foo', query: {} };
const locationWithCWE = { pathname: 'foo', query: { showCWE: 'true' } };
const owaspParams = { type: 'owasp_top_10' };
const sansParams = { type: 'sans_top_25' };
const wrongParams = { type: 'foo' };

beforeEach(() => {
  getSecurityHotspots.mockClear();
});

it('renders error on wrong type parameters', () => {
  const wrapper = shallow(
    <App
      component={component}
      location={location}
      params={wrongParams}
      router={{ push: jest.fn() }}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('renders owaspTop10', async () => {
  const wrapper = shallow(
    <App
      component={component}
      location={location}
      params={owaspParams}
      router={{ push: jest.fn() }}
    />
  );
  await waitAndUpdate(wrapper);
  expect(getSecurityHotspots).toBeCalledWith({
    project: 'foo',
    standard: 'owaspTop10',
    includeDistribution: false,
    branch: undefined
  });
  expect(wrapper).toMatchSnapshot();
});

it('renders with cwe', () => {
  const wrapper = shallow(
    <App
      component={component}
      location={locationWithCWE}
      params={owaspParams}
      router={{ push: jest.fn() }}
    />
  );
  expect(getSecurityHotspots).toBeCalledWith({
    project: 'foo',
    standard: 'owaspTop10',
    includeDistribution: true,
    branch: undefined
  });
  expect(wrapper).toMatchSnapshot();
});

it('handle checkbox for cwe display', async () => {
  const wrapper = shallow(
    <App
      component={component}
      location={location}
      params={owaspParams}
      router={{ push: jest.fn() }}
    />
  );
  expect(getSecurityHotspots).toBeCalledWith({
    project: 'foo',
    standard: 'owaspTop10',
    includeDistribution: false,
    branch: undefined
  });
  expect(wrapper).toMatchSnapshot();

  wrapper.find('Checkbox').prop<Function>('onCheck')(true);
  await waitAndUpdate(wrapper);

  expect(getSecurityHotspots).toBeCalledWith({
    project: 'foo',
    standard: 'owaspTop10',
    includeDistribution: true,
    branch: undefined
  });
  expect(wrapper).toMatchSnapshot();
});

it('renders sansTop25', () => {
  const wrapper = shallow(
    <App
      component={component}
      location={location}
      params={sansParams}
      router={{ push: jest.fn() }}
    />
  );
  expect(getSecurityHotspots).toBeCalledWith({
    project: 'foo',
    standard: 'sansTop25',
    includeDistribution: false,
    branch: undefined
  });
  expect(wrapper).toMatchSnapshot();
});
