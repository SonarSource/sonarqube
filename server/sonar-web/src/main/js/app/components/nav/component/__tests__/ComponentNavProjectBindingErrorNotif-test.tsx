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
import { mockComponent } from '../../../../../helpers/mocks/component';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import ComponentNavProjectBindingErrorNotif, {
  ComponentNavProjectBindingErrorNotifProps,
} from '../ComponentNavProjectBindingErrorNotif';

it('should not show a link if use is not allowed', () => {
  renderComponentNavProjectBindingErrorNotif({
    component: mockComponent({ configuration: { showSettings: false } }),
  });
  expect(
    screen.queryByRole('link', {
      name: 'component_navigation.pr_deco.action.check_project_settings',
    }),
  ).not.toBeInTheDocument();
});

it('should show a link if use is allowed', () => {
  renderComponentNavProjectBindingErrorNotif({
    component: mockComponent({ configuration: { showSettings: true } }),
  });
  expect(
    screen.getByRole('link', {
      name: 'component_navigation.pr_deco.action.check_project_settings',
    }),
  ).toBeInTheDocument();
});

function renderComponentNavProjectBindingErrorNotif(
  props: Partial<ComponentNavProjectBindingErrorNotifProps> = {},
) {
  return renderComponent(
    <ComponentNavProjectBindingErrorNotif component={mockComponent()} {...props} />,
  );
}
