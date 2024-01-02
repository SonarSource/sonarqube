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
import { fetchWebApi } from '../../../../api/web-api';
import { addSideBarClass, removeSideBarClass } from '../../../../helpers/pages';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { WebApiApp } from '../WebApiApp';

jest.mock('../../../../components/common/ScreenPositionHelper');

jest.mock('../../../../api/web-api', () => ({
  fetchWebApi: jest.fn().mockResolvedValue([
    {
      actions: [],
      description: 'foo',
      internal: true,
      path: 'foo/bar',
      since: '1.0',
    },
  ]),
}));

jest.mock('../../../../helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  removeSideBarClass: jest.fn(),
}));

it('should render correctly', async () => {
  (global as any).scrollTo = jest.fn();

  const wrapper = shallowRender();

  expect(addSideBarClass).toHaveBeenCalled();
  expect(fetchWebApi).toHaveBeenCalled();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('ScreenPositionHelper').dive()).toMatchSnapshot();

  wrapper.unmount();
  expect(removeSideBarClass).toHaveBeenCalled();
});

function shallowRender(props: Partial<WebApiApp['props']> = {}) {
  return shallow(
    <WebApiApp
      location={mockLocation()}
      params={{ splat: 'foo/bar' }}
      router={mockRouter()}
      {...props}
    />
  );
}
