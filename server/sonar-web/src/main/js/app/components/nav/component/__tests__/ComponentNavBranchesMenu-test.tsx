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
import { shallow } from 'enzyme';
import * as React from 'react';
import { elementKeydown } from 'sonar-ui-common/helpers/testUtils';
import {
  mockLongLivingBranch,
  mockMainBranch,
  mockPullRequest,
  mockShortLivingBranch
} from '../../../../../helpers/testMocks';
import { ComponentNavBranchesMenu } from '../ComponentNavBranchesMenu';

const component = { key: 'component' } as T.Component;

it('renders list', () => {
  expect(
    shallow(
      <ComponentNavBranchesMenu
        branchLikes={[
          mockMainBranch(),
          shortBranch('foo'),
          mockLongLivingBranch({ name: 'bar' }),
          shortBranch('baz', true),
          mockPullRequest({ status: { qualityGateStatus: 'OK' }, title: 'qux' })
        ]}
        component={component}
        currentBranchLike={mockMainBranch()}
        onClose={jest.fn()}
        router={{ push: jest.fn() }}
      />
    )
  ).toMatchSnapshot();
});

it('searches', () => {
  const wrapper = shallow(
    <ComponentNavBranchesMenu
      branchLikes={[
        mockMainBranch(),
        shortBranch('foo'),
        shortBranch('foobar'),
        mockLongLivingBranch({ name: 'bar' }),
        mockLongLivingBranch({ name: 'BARBAZ' })
      ]}
      component={component}
      currentBranchLike={mockMainBranch()}
      onClose={jest.fn()}
      router={{ push: jest.fn() }}
    />
  );
  wrapper.setState({ query: 'bar' });
  expect(wrapper).toMatchSnapshot();
});

it('selects next & previous', () => {
  const wrapper = shallow<ComponentNavBranchesMenu>(
    <ComponentNavBranchesMenu
      branchLikes={[
        mockMainBranch(),
        shortBranch('foo'),
        shortBranch('foobar'),
        mockLongLivingBranch({ name: 'bar' })
      ]}
      component={component}
      currentBranchLike={mockMainBranch()}
      onClose={jest.fn()}
      router={{ push: jest.fn() }}
    />
  );
  elementKeydown(wrapper.find('SearchBox'), 40);
  wrapper.update();
  expect(wrapper.state().selected).toEqual(shortBranch('foo'));
  elementKeydown(wrapper.find('SearchBox'), 40);
  wrapper.update();
  expect(wrapper.state().selected).toEqual(shortBranch('foobar'));
  elementKeydown(wrapper.find('SearchBox'), 38);
  wrapper.update();
  expect(wrapper.state().selected).toEqual(shortBranch('foo'));
});

function shortBranch(name: string, isOrphan?: true) {
  return mockShortLivingBranch({ name, isOrphan, status: { qualityGateStatus: 'OK' } });
}
