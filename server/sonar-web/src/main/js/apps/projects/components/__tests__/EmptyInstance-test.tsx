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
import EmptyInstance from '../EmptyInstance';

jest.mock('../../../../helpers/system', () => ({
  isSonarCloud: jest.fn()
}));

it('renders correctly for SQ', () => {
  (isSonarCloud as jest.Mock<any>).mockReturnValue(false);
  expect(
    shallow(
      <EmptyInstance
        currentUser={{ isLoggedIn: false }}
        openProjectOnboarding={jest.fn()}
        organization={undefined}
      />
    )
  ).toMatchSnapshot();
  expect(
    shallow(
      <EmptyInstance
        currentUser={{ isLoggedIn: true, permissions: { global: ['provisioning'] } }}
        openProjectOnboarding={jest.fn()}
        organization={undefined}
      />
    )
  ).toMatchSnapshot();
});

it('renders correctly for SC', () => {
  (isSonarCloud as jest.Mock<any>).mockReturnValue(true);
  expect(
    shallow(
      <EmptyInstance
        currentUser={{ isLoggedIn: false }}
        openProjectOnboarding={jest.fn()}
        organization={{ key: 'foo', name: 'Foo' }}
      />
    )
  ).toMatchSnapshot();
  expect(
    shallow(
      <EmptyInstance
        currentUser={{ isLoggedIn: false }}
        openProjectOnboarding={jest.fn()}
        organization={{ actions: { provision: true }, key: 'foo', name: 'Foo' }}
      />
    )
  ).toMatchSnapshot();
});
