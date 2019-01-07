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
import { EmptyOverview, WarningMessage } from '../EmptyOverview';

const branch = { isMain: true, name: 'b', type: 'LONG' };

const component = {
  key: 'foo',
  analysisDate: '2016-01-01',
  breadcrumbs: [],
  name: 'Foo',
  organization: 'org',
  qualifier: 'TRK',
  version: '0.0.1'
};

const LoggedInUser = {
  isLoggedIn: true,
  login: 'luke',
  name: 'Skywalker'
};

it('renders correctly', () => {
  expect(
    shallow(
      <EmptyOverview
        branchLike={branch}
        branchLikes={[branch]}
        component={component}
        currentUser={LoggedInUser}
        onComponentChange={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});

it('should render another message when there are branches', () => {
  expect(
    shallow(
      <EmptyOverview
        branchLike={branch}
        branchLikes={[branch, branch]}
        component={component}
        currentUser={LoggedInUser}
        onComponentChange={jest.fn()}
      />
    )
  ).toMatchSnapshot();
  expect(
    shallow(
      <EmptyOverview
        branchLike={branch}
        branchLikes={[branch, branch, branch]}
        component={component}
        currentUser={LoggedInUser}
        onComponentChange={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});

it('should not render the tutorial', () => {
  expect(
    shallow(
      <EmptyOverview
        branchLike={branch}
        branchLikes={[branch]}
        component={component}
        currentUser={LoggedInUser}
        hasAnalyses={true}
        onComponentChange={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});

it('should render warning message', () => {
  expect(shallow(<WarningMessage branchLike={branch} message="foo" />)).toMatchSnapshot();
});

it('should not render warning message', () => {
  expect(
    shallow(
      <WarningMessage
        branchLike={{
          base: 'foo',
          branch: 'bar',
          key: '1',
          title: 'PR bar'
        }}
        message="foo"
      />
    )
      .find('FormattedMessage')
      .exists()
  ).toBeFalsy();
});
