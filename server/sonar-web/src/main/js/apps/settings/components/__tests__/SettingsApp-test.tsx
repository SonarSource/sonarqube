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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getDefinitions } from '../../../../api/settings';
import { mockComponent } from '../../../../helpers/mocks/component';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass,
} from '../../../../helpers/pages';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { SettingsApp } from '../SettingsApp';

jest.mock('../../../../helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  addWhitePageClass: jest.fn(),
  removeSideBarClass: jest.fn(),
  removeWhitePageClass: jest.fn(),
}));

jest.mock('../../../../api/settings', () => ({
  getDefinitions: jest.fn().mockResolvedValue([]),
}));

it('should render default view correctly', async () => {
  const wrapper = shallowRender();

  expect(addSideBarClass).toHaveBeenCalled();
  expect(addWhitePageClass).toHaveBeenCalled();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  expect(getDefinitions).toHaveBeenCalledWith(undefined);

  wrapper.unmount();

  expect(removeSideBarClass).toHaveBeenCalled();
  expect(removeWhitePageClass).toHaveBeenCalled();
});

it('should fetch definitions for component', async () => {
  const key = 'component-key';
  const wrapper = shallowRender({ component: mockComponent({ key }) });

  await waitAndUpdate(wrapper);
  expect(getDefinitions).toHaveBeenCalledWith(key);
});

function shallowRender(props: Partial<SettingsApp['props']> = {}) {
  return shallow(<SettingsApp {...props} />);
}
