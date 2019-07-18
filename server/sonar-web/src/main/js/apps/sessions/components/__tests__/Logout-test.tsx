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
import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { Logout } from '../Logout';

it('should logout correctly', async () => {
  const doLogout = jest.fn().mockResolvedValue(true);
  window.location.replace = jest.fn();

  const wrapper = shallowRender({ doLogout });
  await waitAndUpdate(wrapper);

  expect(doLogout).toHaveBeenCalled();
  expect(window.location.replace).toHaveBeenCalledWith('/');
});

it('should not redirect if logout fails', async () => {
  const doLogout = jest.fn().mockRejectedValue(false);
  window.location.replace = jest.fn();

  const wrapper = shallowRender({ doLogout });
  await waitAndUpdate(wrapper);

  expect(doLogout).toHaveBeenCalled();
  expect(window.location.replace).not.toHaveBeenCalled();
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<Logout['props']> = {}) {
  return shallow(<Logout doLogout={jest.fn()} {...props} />);
}
