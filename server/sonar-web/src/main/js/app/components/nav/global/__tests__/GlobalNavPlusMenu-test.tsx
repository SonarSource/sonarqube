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
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import { AlmKeys } from '../../../../../types/alm-settings';
import { ComponentQualifier } from '../../../../../types/component';
import GlobalNavPlusMenu, { GlobalNavPlusMenuProps } from '../GlobalNavPlusMenu';

it('should render correctly', () => {
  expect(shallowRender({ canCreateApplication: true })).toMatchSnapshot('app only');
  expect(shallowRender({ canCreatePortfolio: true })).toMatchSnapshot('portfolio only');
  expect(shallowRender({ canCreateProject: true })).toMatchSnapshot('project only');
  expect(
    shallowRender({ canCreateProject: true, compatibleAlms: [AlmKeys.Bitbucket] })
  ).toMatchSnapshot('imports');
  expect(
    shallowRender({
      canCreateApplication: true,
      canCreatePortfolio: true,
      canCreateProject: true,
      compatibleAlms: [AlmKeys.Bitbucket]
    })
  ).toMatchSnapshot('all');
});

it('should trigger onClick', () => {
  const onComponentCreationClick = jest.fn();
  const wrapper = shallowRender({
    canCreateApplication: true,
    canCreatePortfolio: true,
    onComponentCreationClick
  });

  // Portfolio
  const portfolioButton = wrapper.find(ButtonLink).at(0);
  portfolioButton.simulate('click');
  expect(onComponentCreationClick).toBeCalledWith(ComponentQualifier.Portfolio);

  onComponentCreationClick.mockClear();

  // App
  const appButton = wrapper.find(ButtonLink).at(1);
  appButton.simulate('click');
  expect(onComponentCreationClick).toBeCalledWith(ComponentQualifier.Application);
});

function shallowRender(overrides: Partial<GlobalNavPlusMenuProps> = {}) {
  return shallow(
    <GlobalNavPlusMenu
      canCreateApplication={false}
      canCreatePortfolio={false}
      canCreateProject={false}
      compatibleAlms={[]}
      onComponentCreationClick={jest.fn()}
      {...overrides}
    />
  );
}
