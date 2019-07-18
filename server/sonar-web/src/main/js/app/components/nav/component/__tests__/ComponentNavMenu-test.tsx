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
import { ComponentNavMenu } from '../ComponentNavMenu';

const mainBranch: T.MainBranch = { isMain: true, name: 'master' };

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
  const wrapper = shallow(
    <ComponentNavMenu
      appState={{ branchesEnabled: true }}
      branchLike={mainBranch}
      component={component}
    />
  );
  expect(wrapper.find('Dropdown[data-test="extensions"]')).toMatchSnapshot();
  expect(wrapper.find('Dropdown[data-test="administration"]')).toMatchSnapshot();
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
  const wrapper = shallow(
    <ComponentNavMenu
      appState={{ branchesEnabled: true }}
      branchLike={mainBranch}
      component={component}
    />
  );
  expect(wrapper.find('Dropdown[data-test="extensions"]')).toMatchSnapshot();
  expect(wrapper.find('Dropdown[data-test="administration"]')).toMatchSnapshot();
});

it('should render correctly for security extensions', () => {
  const component = {
    ...baseComponent,
    configuration: {
      showSettings: true,
      extensions: [{ key: 'securityreport/foo', name: 'Foo' }, { key: 'bar', name: 'Bar' }]
    },
    extensions: [
      { key: 'securityreport/foo', name: 'ComponentFoo' },
      { key: 'component-bar', name: 'ComponentBar' }
    ]
  };
  const wrapper = shallow(
    <ComponentNavMenu
      appState={{ branchesEnabled: true }}
      branchLike={mainBranch}
      component={component}
    />
  );
  expect(wrapper.find('Dropdown[data-test="extensions"]')).toMatchSnapshot();
  expect(wrapper.find('Dropdown[data-test="security"]')).toMatchSnapshot();
});

it('should work for short-living branches', () => {
  const branch: T.ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'feature',
    type: 'SHORT'
  };
  const component = {
    ...baseComponent,
    configuration: { showSettings: true },
    extensions: [{ key: 'component-foo', name: 'ComponentFoo' }]
  };
  expect(
    shallow(
      <ComponentNavMenu
        appState={{ branchesEnabled: true }}
        branchLike={branch}
        component={component}
      />
    )
  ).toMatchSnapshot();
});

it('should work for long-living branches', () => {
  const branch: T.LongLivingBranch = { isMain: false, name: 'release', type: 'LONG' };
  [true, false].forEach(showSettings =>
    expect(
      shallow(
        <ComponentNavMenu
          appState={{ branchesEnabled: true }}
          branchLike={branch}
          component={{
            ...baseComponent,
            configuration: { showSettings },
            extensions: [{ key: 'component-foo', name: 'ComponentFoo' }]
          }}
        />
      )
    ).toMatchSnapshot()
  );
});

it('should work for all qualifiers', () => {
  ['TRK', 'VW', 'SVW', 'APP'].forEach(checkWithQualifier);
  expect.assertions(4);

  function checkWithQualifier(qualifier: string) {
    const component = { ...baseComponent, configuration: { showSettings: true }, qualifier };
    expect(
      shallow(
        <ComponentNavMenu
          appState={{ branchesEnabled: true }}
          branchLike={mainBranch}
          component={component}
        />
      )
    ).toMatchSnapshot();
  }
});
