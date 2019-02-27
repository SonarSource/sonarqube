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
import {
  mockShortLivingBranch,
  mockComponent,
  mockLongLivingBranch,
  mockPullRequest
} from '../../../../../helpers/testMocks';

it('renders status of short-living branch', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders meta for long-living branch', () => {
  expect(shallowRender({ branchLike: mockLongLivingBranch() })).toMatchSnapshot();
});

it('renders meta for pull request', () => {
  expect(
    shallowRender({
      branchLike: mockPullRequest({
        url: 'https://example.com/pull/1234'
      })
    })
  ).toMatchSnapshot();
});

function shallowRender(props = {}) {
  return shallow(
    <ComponentNavMeta
      branchLike={mockShortLivingBranch()}
      component={mockComponent({ analysisDate: '2017-01-02T00:00:00.000Z', version: '0.0.1' })}
      currentUser={{
        isLoggedIn: false
      }}
      warnings={[]}
      {...props}
    />
  );
}
