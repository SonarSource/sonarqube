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
import { ComponentNavBranchesMenu } from '../ComponentNavBranchesMenu';
import { elementKeydown } from '../../../../../helpers/testUtils';

const component = { key: 'component' } as T.Component;

it('renders list', () => {
  expect(
    shallow(
      <ComponentNavBranchesMenu
        branchLikes={[
          mainBranch(),
          shortBranch('foo'),
          longBranch('bar'),
          shortBranch('baz', true),
          pullRequest('qux')
        ]}
        component={component}
        currentBranchLike={mainBranch()}
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
        mainBranch(),
        shortBranch('foo'),
        shortBranch('foobar'),
        longBranch('bar'),
        longBranch('BARBAZ')
      ]}
      component={component}
      currentBranchLike={mainBranch()}
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
      branchLikes={[mainBranch(), shortBranch('foo'), shortBranch('foobar'), longBranch('bar')]}
      component={component}
      currentBranchLike={mainBranch()}
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

function mainBranch(): T.MainBranch {
  return { isMain: true, name: 'master' };
}

function shortBranch(name: string, isOrphan?: true): T.ShortLivingBranch {
  return {
    isMain: false,
    isOrphan,
    mergeBranch: 'master',
    name,
    status: { qualityGateStatus: 'OK' },
    type: 'SHORT'
  };
}

function longBranch(name: string): T.LongLivingBranch {
  return { isMain: false, name, type: 'LONG' };
}

function pullRequest(title: string): T.PullRequest {
  return {
    base: 'master',
    branch: 'feature',
    key: '1234',
    status: { qualityGateStatus: 'OK' },
    title
  };
}
