/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { screen } from '@testing-library/react';
import * as React from 'react';
import {
  mockBranch,
  mockMainBranch,
  mockPullRequest,
} from '../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../../types/component';
import { Menu } from '../Menu';

const BASE_COMPONENT = mockComponent({
  analysisDate: '2019-12-01',
  key: 'foo',
  name: 'foo',
});

it('should render correctly', () => {
  const component = {
    ...BASE_COMPONENT,
    configuration: {
      showSettings: true,
      extensions: [
        { key: 'foo', name: 'Foo' },
        { key: 'bar', name: 'Bar' },
        { key: 'securityreport/foo', name: 'Foo' },
      ],
    },
    extensions: [
      { key: 'component-foo', name: 'ComponentFoo' },
      { key: 'component-bar', name: 'ComponentBar' },
      { key: 'securityreport/foo', name: 'Security Report' },
    ],
  };
  renderMenu({ component });

  // Security Report is rendered on its own, as is not part of the dropdown menu.
  expect(screen.getByRole('link', { name: 'layout.security_reports' })).toBeInTheDocument();

  // Check the dropdown.
  const button = screen.getByRole('button', { name: 'more' });
  expect(button).toBeInTheDocument();
  button.click();
  expect(screen.getByRole('link', { name: 'ComponentFoo' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'ComponentBar' })).toBeInTheDocument();
});

it('should render correctly when on a branch', () => {
  renderMenu({
    branchLike: mockBranch(),
    component: {
      ...BASE_COMPONENT,
      configuration: { showSettings: true },
      extensions: [{ key: 'component-foo', name: 'ComponentFoo' }],
    },
  });

  expect(screen.getByRole('link', { name: 'overview.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'issues.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'layout.measures' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'project.info.title' })).toBeInTheDocument();

  // If on a branch, regardless is the user is an admin or not, we do not show
  // the settings link.
  expect(
    screen.queryByRole('link', { name: `layout.settings.${ComponentQualifier.Project}` })
  ).not.toBeInTheDocument();
});

it('should render correctly when on a pull request', () => {
  renderMenu({
    branchLike: mockPullRequest(),
    component: {
      ...BASE_COMPONENT,
      configuration: { showSettings: true },
      extensions: [{ key: 'component-foo', name: 'ComponentFoo' }],
    },
  });

  expect(screen.getByRole('link', { name: 'overview.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'issues.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'layout.measures' })).toBeInTheDocument();

  expect(
    screen.queryByRole('link', { name: `layout.settings.${ComponentQualifier.Project}` })
  ).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'project.info.title' })).not.toBeInTheDocument();
});

it('should disable links if no analysis has been done', () => {
  renderMenu({
    component: {
      ...BASE_COMPONENT,
      analysisDate: undefined,
    },
  });
  expect(screen.getByRole('link', { name: 'overview.page' })).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'issues.page' })).not.toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'layout.measures' })).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'project.info.title' })).toBeInTheDocument();
});

it('should disable links if application has inaccessible projects', () => {
  renderMenu({
    component: {
      ...BASE_COMPONENT,
      qualifier: ComponentQualifier.Application,
      canBrowseAllChildProjects: false,
    },
  });
  expect(screen.queryByRole('link', { name: 'overview.page' })).not.toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'issues.page' })).not.toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'layout.measures' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'application.info.title' })).not.toBeInTheDocument();
});

function renderMenu(props: Partial<Menu['props']> = {}) {
  const mainBranch = mockMainBranch();
  return renderComponent(
    <Menu
      hasFeature={jest.fn().mockReturnValue(false)}
      branchLike={mainBranch}
      branchLikes={[mainBranch]}
      component={BASE_COMPONENT}
      isInProgress={false}
      isPending={false}
      onToggleProjectInfo={jest.fn()}
      projectInfoDisplayed={false}
      {...props}
    />
  );
}
