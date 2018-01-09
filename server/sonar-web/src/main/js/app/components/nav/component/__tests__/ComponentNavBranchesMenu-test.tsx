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
import * as React from 'react';
import { shallow } from 'enzyme';
import ComponentNavBranchesMenu from '../ComponentNavBranchesMenu';
import {
  BranchType,
  MainBranch,
  ShortLivingBranch,
  LongLivingBranch,
  Component
} from '../../../../types';
import { elementKeydown } from '../../../../../helpers/testUtils';

const component = { key: 'component' } as Component;

it('renders list', () => {
  expect(
    shallow(
      <ComponentNavBranchesMenu
        branches={[mainBranch(), shortBranch('foo'), longBranch('bar'), shortBranch('baz', true)]}
        component={component}
        currentBranch={mainBranch()}
        onClose={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});

it('searches', () => {
  const wrapper = shallow(
    <ComponentNavBranchesMenu
      branches={[mainBranch(), shortBranch('foo'), shortBranch('foobar'), longBranch('bar')]}
      component={component}
      currentBranch={mainBranch()}
      onClose={jest.fn()}
    />
  );
  wrapper.setState({ query: 'bar' });
  expect(wrapper).toMatchSnapshot();
});

it('selects next & previous', () => {
  const wrapper = shallow(
    <ComponentNavBranchesMenu
      branches={[mainBranch(), shortBranch('foo'), shortBranch('foobar'), longBranch('bar')]}
      component={component}
      currentBranch={mainBranch()}
      onClose={jest.fn()}
    />
  );
  elementKeydown(wrapper.find('SearchBox'), 40);
  wrapper.update();
  expect(wrapper.state().selected).toBe('foo');
  elementKeydown(wrapper.find('SearchBox'), 40);
  wrapper.update();
  expect(wrapper.state().selected).toBe('foobar');
  elementKeydown(wrapper.find('SearchBox'), 38);
  wrapper.update();
  expect(wrapper.state().selected).toBe('foo');
});

function mainBranch(): MainBranch {
  return { isMain: true, name: 'master' };
}

function shortBranch(name: string, isOrphan?: true): ShortLivingBranch {
  return {
    isMain: false,
    isOrphan,
    mergeBranch: 'master',
    name,
    status: { bugs: 0, codeSmells: 0, vulnerabilities: 0 },
    type: BranchType.SHORT
  };
}

function longBranch(name: string): LongLivingBranch {
  return { isMain: false, name, type: BranchType.LONG };
}
