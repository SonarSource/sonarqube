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
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../../types/component';
import ComponentNav, { ComponentNavProps } from '../ComponentNav';

it('renders correctly when the project binding is incorrect', () => {
  renderComponentNav({
    projectBindingErrors: mockProjectAlmBindingConfigurationErrors(),
  });
  expect(
    screen.getByText('component_navigation.pr_deco.error_detected_X', { exact: false }),
  ).toBeInTheDocument();
});

it('correctly returns focus to the Project Information link when the drawer is closed', async () => {
  renderComponentNav();
  screen.getByRole('link', { name: 'project.info.title' }).click();
  expect(await screen.findByText('/project/information?id=my-project')).toBeInTheDocument();
});

function renderComponentNav(props: Partial<ComponentNavProps> = {}) {
  return renderApp(
    '/',
    <ComponentNav
      component={mockComponent({
        breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }],
      })}
      isInProgress={false}
      isPending={false}
      {...props}
    />,
  );
}
