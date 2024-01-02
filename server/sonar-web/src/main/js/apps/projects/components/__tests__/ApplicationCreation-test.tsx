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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { getComponentNavigation } from '../../../../api/navigation';
import CreateApplicationForm from '../../../../app/components/extensions/CreateApplicationForm';
import { Button } from '../../../../components/controls/buttons';
import { mockAppState, mockLoggedInUser, mockRouter } from '../../../../helpers/testMocks';
import { queryToSearch } from '../../../../helpers/urls';
import { ComponentQualifier } from '../../../../types/component';
import { ApplicationCreation, ApplicationCreationProps } from '../ApplicationCreation';

jest.mock('../../../../api/navigation', () => ({
  getComponentNavigation: jest.fn().mockResolvedValue({}),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('available');
  expect(
    shallowRender({ appState: mockAppState({ qualifiers: [ComponentQualifier.Portfolio] }) })
  ).toMatchSnapshot('unavailable');
  expect(
    shallowRender({ currentUser: mockLoggedInUser({ permissions: { global: ['otherrights'] } }) })
  ).toMatchSnapshot('not allowed');
});

it('should show form and callback when submitted - admin', async () => {
  (getComponentNavigation as jest.Mock).mockResolvedValueOnce({
    configuration: { showSettings: true },
  });
  const routerPush = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ push: routerPush }) });

  await openAndSubmitForm(wrapper);

  expect(routerPush).toHaveBeenCalledWith({
    pathname: '/project/admin/extension/developer-server/application-console',
    search: queryToSearch({
      id: 'new app',
    }),
  });
});

it('should show form and callback when submitted - user', async () => {
  (getComponentNavigation as jest.Mock).mockResolvedValueOnce({
    configuration: { showSettings: false },
  });
  const routerPush = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ push: routerPush }) });

  await openAndSubmitForm(wrapper);

  expect(routerPush).toHaveBeenCalledWith({
    pathname: '/dashboard',
    search: queryToSearch({
      id: 'new app',
    }),
  });
});

async function openAndSubmitForm(wrapper: ShallowWrapper) {
  wrapper.find(Button).simulate('click');

  const creationForm = wrapper.find(CreateApplicationForm);
  expect(creationForm.exists()).toBe(true);

  await creationForm
    .props()
    .onCreate({ key: 'new app', qualifier: ComponentQualifier.Application });
  expect(getComponentNavigation).toHaveBeenCalled();
  expect(wrapper.find(CreateApplicationForm).exists()).toBe(false);
}

function shallowRender(overrides: Partial<ApplicationCreationProps> = {}) {
  return shallow(
    <ApplicationCreation
      appState={mockAppState({ qualifiers: [ComponentQualifier.Application] })}
      currentUser={mockLoggedInUser({ permissions: { global: ['applicationcreator'] } })}
      router={mockRouter()}
      {...overrides}
    />
  );
}
