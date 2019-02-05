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
import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import { GlobalNavPlus } from '../GlobalNavPlus';
import { isSonarCloud } from '../../../../../helpers/system';
import { mockRouter } from '../../../../../helpers/testMocks';
import { click } from '../../../../../helpers/testUtils';

jest.mock('../../../../../helpers/system', () => ({
  isSonarCloud: jest.fn()
}));

beforeEach(() => {
  (isSonarCloud as jest.Mock).mockReturnValue(false);
});

it('render', () => {
  const wrapper = getWrapper();
  expect(wrapper.find('Dropdown')).toMatchSnapshot();
});

it('opens onboarding', () => {
  const openProjectOnboarding = jest.fn();
  const wrapper = getOverlayWrapper(getWrapper({ openProjectOnboarding }));
  click(wrapper.find('.js-new-project'));
  expect(openProjectOnboarding).toBeCalled();
});

it('should display create new project link when user has permission only', () => {
  expect(getWrapper({}, []).find('Dropdown').length).toEqual(0);
});

it('should display create new organization on SonarCloud only', () => {
  (isSonarCloud as jest.Mock).mockReturnValue(true);
  expect(getOverlayWrapper(getWrapper())).toMatchSnapshot();
});

it('should display new organization and new project on SonarCloud', () => {
  (isSonarCloud as jest.Mock).mockReturnValue(true);
  expect(getOverlayWrapper(getWrapper({}, []))).toMatchSnapshot();
});

it('should display create portfolio and application', () => {
  checkOpenCreatePortfolio(['applicationcreator', 'portfoliocreator'], undefined);
});

it('should display create portfolio', () => {
  checkOpenCreatePortfolio(['portfoliocreator'], 'VW');
});

it('should display create application', () => {
  checkOpenCreatePortfolio(['applicationcreator'], 'APP');
});

function getWrapper(props = {}, globalPermissions?: string[]) {
  return shallow(
    // @ts-ignore avoid passing everything from WithRouterProps
    <GlobalNavPlus
      appState={{ qualifiers: [] }}
      currentUser={
        {
          isLoggedIn: true,
          permissions: { global: globalPermissions || ['provisioning'] }
        } as T.LoggedInUser
      }
      openProjectOnboarding={jest.fn()}
      router={mockRouter()}
      {...props}
    />
  );
}

function getOverlayWrapper(wrapper: ShallowWrapper) {
  return shallow(wrapper.find('Dropdown').prop('overlay'));
}

function checkOpenCreatePortfolio(permissions: string[], defaultQualifier?: string) {
  const wrapper = getWrapper({ appState: { qualifiers: ['VW'] } }, permissions);
  wrapper.setState({ governanceReady: true });
  const overlayWrapper = getOverlayWrapper(wrapper);
  expect(overlayWrapper.find('.js-new-portfolio')).toMatchSnapshot();

  click(overlayWrapper.find('.js-new-portfolio'));
  wrapper.update();
  expect(wrapper.find('CreateFormShim').exists()).toBe(true);
  expect(wrapper.find('CreateFormShim').prop('defaultQualifier')).toBe(defaultQualifier);
}
