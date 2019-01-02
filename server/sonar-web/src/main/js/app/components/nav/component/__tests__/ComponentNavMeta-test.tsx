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
import { ComponentNavMeta } from '../ComponentNavMeta';

const COMPONENT = {
  analysisDate: '2017-01-02T00:00:00.000Z',
  breadcrumbs: [],
  key: 'foo',
  name: 'Foo',
  organization: 'org',
  qualifier: 'TRK',
  version: '0.0.1'
};

const MEASURES = [
  { metric: 'new_coverage', value: '0', periods: [{ index: 1, value: '95.9943' }] },
  { metric: 'new_duplicated_lines_density', periods: [{ index: 1, value: '3.5' }] }
];

it('renders status of short-living branch', () => {
  const branch: T.ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'feature',
    status: { bugs: 0, codeSmells: 2, qualityGateStatus: 'ERROR', vulnerabilities: 3 },
    type: 'SHORT'
  };
  expect(
    shallow(
      <ComponentNavMeta
        branchLike={branch}
        branchMeasures={MEASURES}
        component={COMPONENT}
        currentUser={{ isLoggedIn: false }}
        warnings={[]}
      />
    )
  ).toMatchSnapshot();
});

it('renders meta for long-living branch', () => {
  const branch: T.LongLivingBranch = {
    isMain: false,
    name: 'release',
    status: { qualityGateStatus: 'OK' },
    type: 'LONG'
  };
  expect(
    shallow(
      <ComponentNavMeta
        branchLike={branch}
        component={COMPONENT}
        currentUser={{ isLoggedIn: false }}
        warnings={[]}
      />
    )
  ).toMatchSnapshot();
});

it('renders meta for pull request', () => {
  const pullRequest: T.PullRequest = {
    base: 'master',
    branch: 'feature',
    key: '1234',
    status: { bugs: 0, codeSmells: 2, qualityGateStatus: 'ERROR', vulnerabilities: 3 },
    title: 'Feature PR',
    url: 'https://example.com/pull/1234'
  };
  expect(
    shallow(
      <ComponentNavMeta
        branchLike={pullRequest}
        component={COMPONENT}
        currentUser={{ isLoggedIn: false }}
        warnings={[]}
      />
    )
  ).toMatchSnapshot();
});
