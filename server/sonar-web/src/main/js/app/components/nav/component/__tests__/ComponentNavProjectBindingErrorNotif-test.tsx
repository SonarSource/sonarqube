/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { mockProjectAlmBindingConfigurationErrors } from '../../../../../helpers/mocks/alm-settings';
import { mockComponent, mockCurrentUser, mockLoggedInUser } from '../../../../../helpers/testMocks';
import {
  AlmKeys,
  ProjectAlmBindingConfigurationErrorScope
} from '../../../../../types/alm-settings';
import { Permissions } from '../../../../../types/permissions';
import {
  ComponentNavProjectBindingErrorNotif,
  ComponentNavProjectBindingErrorNotifProps
} from '../ComponentNavProjectBindingErrorNotif';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('global, no admin');
  expect(
    shallowRender({
      currentUser: mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })
    })
  ).toMatchSnapshot('global, admin');
  expect(
    shallowRender({
      projectBindingErrors: mockProjectAlmBindingConfigurationErrors({
        scope: ProjectAlmBindingConfigurationErrorScope.Project
      })
    })
  ).toMatchSnapshot('project, no admin');
  expect(
    shallowRender({
      component: mockComponent({ configuration: { showSettings: true } }),
      projectBindingErrors: mockProjectAlmBindingConfigurationErrors({
        scope: ProjectAlmBindingConfigurationErrorScope.Project
      })
    })
  ).toMatchSnapshot('project, admin');
  expect(
    shallowRender({
      projectBindingErrors: mockProjectAlmBindingConfigurationErrors({
        scope: ProjectAlmBindingConfigurationErrorScope.Unknown
      })
    })
  ).toMatchSnapshot('unknown');
});

function shallowRender(props: Partial<ComponentNavProjectBindingErrorNotifProps> = {}) {
  return shallow<ComponentNavProjectBindingErrorNotifProps>(
    <ComponentNavProjectBindingErrorNotif
      alm={AlmKeys.GitHub}
      component={mockComponent()}
      currentUser={mockCurrentUser()}
      projectBindingErrors={mockProjectAlmBindingConfigurationErrors()}
      {...props}
    />
  );
}
