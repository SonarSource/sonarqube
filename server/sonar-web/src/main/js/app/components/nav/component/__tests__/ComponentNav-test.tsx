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
import React from 'react';
import { mockProjectAlmBindingConfigurationErrors } from '../../../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockTask, mockTaskWarning } from '../../../../../helpers/mocks/tasks';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../../types/component';
import { TaskStatuses } from '../../../../../types/tasks';
import ComponentNav, { ComponentNavProps } from '../ComponentNav';

it('renders correctly when there are warnings', () => {
  renderComponentNav({ warnings: [mockTaskWarning()] });
  expect(
    screen.getByText('component_navigation.last_analysis_had_warnings', { exact: false })
  ).toBeInTheDocument();
});

it('renders correctly when there is a background task in progress', () => {
  renderComponentNav({ isInProgress: true });
  expect(
    screen.getByText('component_navigation.status.in_progress', { exact: false })
  ).toBeInTheDocument();
});

it('renders correctly when there is a background task pending', () => {
  renderComponentNav({ isPending: true });
  expect(
    screen.getByText('component_navigation.status.pending', { exact: false })
  ).toBeInTheDocument();
});

it('renders correctly when there is a failing background task', () => {
  renderComponentNav({ currentTask: mockTask({ status: TaskStatuses.Failed }) });
  expect(
    screen.getByText('component_navigation.status.failed_X', { exact: false })
  ).toBeInTheDocument();
});

it('renders correctly when the project binding is incorrect', () => {
  renderComponentNav({
    projectBindingErrors: mockProjectAlmBindingConfigurationErrors(),
  });
  expect(
    screen.getByText('component_navigation.pr_deco.error_detected_X', { exact: false })
  ).toBeInTheDocument();
});

it('correctly returns focus to the Project Information link when the drawer is closed', () => {
  renderComponentNav();
  screen.getByRole('button', { name: 'project.info.title' }).click();
  expect(screen.getByRole('button', { name: 'project.info.title' })).not.toHaveFocus();

  screen.getByRole('button', { name: 'close' }).click();
  expect(screen.getByRole('button', { name: 'project.info.title' })).toHaveFocus();
});

function renderComponentNav(props: Partial<ComponentNavProps> = {}) {
  return renderApp(
    '/',
    <ComponentNav
      branchLikes={[]}
      component={mockComponent({
        breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }],
      })}
      currentBranchLike={undefined}
      isInProgress={false}
      isPending={false}
      onComponentChange={jest.fn()}
      onWarningDismiss={jest.fn()}
      warnings={[]}
      {...props}
    />
  );
}
