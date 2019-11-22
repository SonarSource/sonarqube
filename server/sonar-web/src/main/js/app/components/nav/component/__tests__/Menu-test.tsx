/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
/* eslint-disable sonarjs/no-duplicate-string */
import { shallow } from 'enzyme';
import * as React from 'react';
import {
  mockBranch,
  mockMainBranch,
  mockPullRequest
} from '../../../../../helpers/mocks/branch-like';
import { ComponentQualifier } from '../../../../../types/component';
import { Menu } from '../Menu';

const mainBranch = mockMainBranch();

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
  const wrapper = shallowRender({ component });
  expect(wrapper.find('Dropdown[data-test="extensions"]')).toMatchSnapshot();
  expect(wrapper.find('Dropdown[data-test="administration"]')).toMatchSnapshot();
});

it('should work with multiple extensions', () => {
  const component = {
    ...baseComponent,
    configuration: {
      showSettings: true,
      extensions: [
        { key: 'foo', name: 'Foo' },
        { key: 'bar', name: 'Bar' }
      ]
    },
    extensions: [
      { key: 'component-foo', name: 'ComponentFoo' },
      { key: 'component-bar', name: 'ComponentBar' }
    ]
  };
  const wrapper = shallowRender({ component });
  expect(wrapper.find('Dropdown[data-test="extensions"]')).toMatchSnapshot();
  expect(wrapper.find('Dropdown[data-test="administration"]')).toMatchSnapshot();
});

it('should render correctly for security extensions', () => {
  const component = {
    ...baseComponent,
    configuration: {
      showSettings: true,
      extensions: [
        { key: 'securityreport/foo', name: 'Foo' },
        { key: 'bar', name: 'Bar' }
      ]
    },
    extensions: [
      { key: 'securityreport/foo', name: 'ComponentFoo' },
      { key: 'component-bar', name: 'ComponentBar' }
    ]
  };
  const wrapper = shallowRender({ component });
  expect(wrapper.find('Dropdown[data-test="extensions"]')).toMatchSnapshot();
  expect(wrapper.find('Dropdown[data-test="security"]')).toMatchSnapshot();
});

it('should work for a branch', () => {
  const branchLike = mockBranch({
    name: 'release'
  });
  [true, false].forEach(showSettings =>
    expect(
      shallowRender({
        branchLike,
        component: {
          ...baseComponent,
          configuration: { showSettings },
          extensions: [{ key: 'component-foo', name: 'ComponentFoo' }]
        }
      })
    ).toMatchSnapshot()
  );
});

it('should work for pull requests', () => {
  [true, false].forEach(showSettings =>
    expect(
      shallowRender({
        branchLike: mockPullRequest(),
        component: {
          ...baseComponent,
          configuration: { showSettings },
          extensions: [{ key: 'component-foo', name: 'ComponentFoo' }]
        }
      })
    ).toMatchSnapshot()
  );
});

it('should work for all qualifiers', () => {
  [
    ComponentQualifier.Project,
    ComponentQualifier.Portfolio,
    ComponentQualifier.SubPortfolio,
    ComponentQualifier.Application
  ].forEach(checkWithQualifier);
  expect.assertions(4);

  function checkWithQualifier(qualifier: string) {
    const component = { ...baseComponent, configuration: { showSettings: true }, qualifier };
    expect(shallowRender({ component })).toMatchSnapshot();
  }
});

function shallowRender(props: Partial<Menu['props']>) {
  return shallow<Menu>(
    <Menu
      appState={{ branchesEnabled: true }}
      branchLike={mainBranch}
      component={baseComponent}
      onToggleProjectInfo={jest.fn()}
      {...props}
    />
  );
}
