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
import { isSonarCloud } from '../../../../helpers/system';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import CreateProjectPage from '../CreateProjectPage';

jest.mock('../../../../helpers/system', () => ({
  isSonarCloud: jest.fn().mockReturnValue(false)
}));

it('should render correctly for SonarQube', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for SonarCloud', () => {
  (isSonarCloud as jest.Mock).mockReturnValue(true);
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props = {}) {
  return shallow(
    <CreateProjectPage
      location={mockLocation()}
      params={{}}
      router={mockRouter()}
      routes={[]}
      {...props}
    />
  );
}
