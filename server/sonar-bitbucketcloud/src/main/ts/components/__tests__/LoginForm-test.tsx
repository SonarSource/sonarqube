/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { shallow } from 'enzyme';
import LoginForm from '../LoginForm';
import { getIdentityProviders } from '../../api';

jest.mock('../../api', () => ({
  getIdentityProviders: jest.fn(() =>
    Promise.resolve({
      identityProviders: [
        {
          key: 'bitbucket',
          name: 'Bitbucket',
          iconPath: 'https://sonarcloud.io/static/authbitbucket/bitbucket.svg',
          backgroundColor: '#205081'
        }
      ]
    })
  )
}));

beforeEach(() => {
  (getIdentityProviders as jest.Mock<any>).mockClear();
});

it('should display correctly', async () => {
  const wrapper = shallow(<LoginForm onReload={jest.fn()} />);
  expect(wrapper).toMatchSnapshot();

  await new Promise(setImmediate);
  wrapper.update();
  expect(getIdentityProviders).toHaveBeenCalled();
  expect(wrapper).toMatchSnapshot();
});

it('should display correctly without the bitbucket login button', async () => {
  (getIdentityProviders as jest.Mock<any>).mockImplementationOnce(() =>
    Promise.resolve({ identityProviders: [] })
  );
  const wrapper = shallow(<LoginForm onReload={jest.fn()} />);
  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});
