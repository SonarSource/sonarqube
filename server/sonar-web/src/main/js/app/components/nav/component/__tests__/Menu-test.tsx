/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import {
  mockBranch,
  mockMainBranch,
  mockPullRequest
} from '../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockAppState } from '../../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../../types/component';
import { Menu } from '../Menu';

const mainBranch = mockMainBranch();

const baseComponent = mockComponent({
  analysisDate: '2019-12-01',
  key: 'foo',
  name: 'foo'
});

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

it.each([
  [ComponentQualifier.Project, false],
  [ComponentQualifier.Portfolio, false],
  [ComponentQualifier.Portfolio, true],
  [ComponentQualifier.SubPortfolio, false],
  [ComponentQualifier.SubPortfolio, true],
  [ComponentQualifier.Application, false]
])('should work for qualifier: %s, %s', (qualifier, enableGovernance) => {
  const component = {
    ...baseComponent,
    canBrowseAllChildProjects: true,
    configuration: { showSettings: true },
    extensions: enableGovernance ? [{ key: 'governance/', name: 'governance' }] : [],
    qualifier
  };
  expect(shallowRender({ component })).toMatchSnapshot();
});

it('should disable links if no analysis has been done', () => {
  expect(
    shallowRender({
      component: {
        ...baseComponent,
        analysisDate: undefined
      }
    })
  ).toMatchSnapshot();
});

it('should disable links if application has inaccessible projects', () => {
  expect(
    shallowRender({
      component: {
        ...baseComponent,
        qualifier: ComponentQualifier.Application,
        canBrowseAllChildProjects: false,
        configuration: { showSettings: true }
      }
    })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<Menu['props']>) {
  return shallow<Menu>(
    <Menu
      appState={mockAppState({ branchesEnabled: true })}
      branchLike={mainBranch}
      branchLikes={[mainBranch]}
      component={baseComponent}
      isInProgress={false}
      isPending={false}
      onToggleProjectInfo={jest.fn()}
      {...props}
    />
  );
}
