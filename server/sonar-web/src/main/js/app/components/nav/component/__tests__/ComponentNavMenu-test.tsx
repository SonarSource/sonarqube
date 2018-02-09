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
import ComponentNavMenu from '../ComponentNavMenu';
import { ShortLivingBranch, BranchType, LongLivingBranch, MainBranch } from '../../../../types';

const mainBranch: MainBranch = { isMain: true, name: 'master' };

const baseComponent = {
  breadcrumbs: [],
  key: 'foo',
  name: 'foo',
  organization: 'org',
  qualifier: 'TRK'
};

it('should work with extensions', () => {
  const component = {
    ...baseComponent,
    configuration: { showSettings: true, extensions: [{ key: 'foo', name: 'Foo' }] },
    extensions: [{ key: 'component-foo', name: 'ComponentFoo' }]
  };
  expect(
    shallow(<ComponentNavMenu branch={mainBranch} component={component} />, {
      context: { branchesEnabled: true }
    })
  ).toMatchSnapshot();
});

it('should work with multiple extensions', () => {
  const component = {
    ...baseComponent,
    configuration: {
      showSettings: true,
      extensions: [{ key: 'foo', name: 'Foo' }, { key: 'bar', name: 'Bar' }]
    },
    extensions: [
      { key: 'component-foo', name: 'ComponentFoo' },
      { key: 'component-bar', name: 'ComponentBar' }
    ]
  };
  expect(
    shallow(<ComponentNavMenu branch={mainBranch} component={component} />, {
      context: { branchesEnabled: true }
    })
  ).toMatchSnapshot();
});

it('should work for short-living branches', () => {
  const branch: ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'feature',
    type: BranchType.SHORT
  };
  const component = {
    ...baseComponent,
    configuration: { showSettings: true },
    extensions: [{ key: 'component-foo', name: 'ComponentFoo' }]
  };
  expect(
    shallow(<ComponentNavMenu branch={branch} component={component} />, {
      context: { branchesEnabled: true }
    })
  ).toMatchSnapshot();
});

it('should work for long-living branches', () => {
  const branch: LongLivingBranch = { isMain: false, name: 'release', type: BranchType.LONG };
  [true, false].forEach(showSettings =>
    expect(
      shallow(
        <ComponentNavMenu
          branch={branch}
          component={{
            ...baseComponent,
            configuration: { showSettings },
            extensions: [{ key: 'component-foo', name: 'ComponentFoo' }]
          }}
        />,
        { context: { branchesEnabled: true } }
      )
    ).toMatchSnapshot()
  );
});

it('should work for all qualifiers', () => {
  ['TRK', 'BRC', 'VW', 'SVW', 'APP'].forEach(checkWithQualifier);
  expect.assertions(5);

  function checkWithQualifier(qualifier: string) {
    const component = { ...baseComponent, configuration: { showSettings: true }, qualifier };
    expect(
      shallow(<ComponentNavMenu branch={mainBranch} component={component} />, {
        context: { branchesEnabled: true }
      })
    ).toMatchSnapshot();
  }
});
