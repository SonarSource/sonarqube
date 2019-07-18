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
import { NoFavoriteProjects } from '../NoFavoriteProjects';

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

it('renders', () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => false);
  expect(
    shallow(<NoFavoriteProjects openProjectOnboarding={jest.fn()} organizations={[]} />)
  ).toMatchSnapshot();
});

it('renders for SonarCloud without organizations', () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => true);
  expect(
    shallow(<NoFavoriteProjects openProjectOnboarding={jest.fn()} organizations={[]} />)
  ).toMatchSnapshot();
});

it('renders for SonarCloud with organizations', () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => true);
  const organizations: T.Organization[] = [
    { actions: { admin: true }, key: 'org1', name: 'org1', projectVisibility: 'public' },
    { actions: { admin: false }, key: 'org2', name: 'org2', projectVisibility: 'public' }
  ];
  expect(
    shallow(<NoFavoriteProjects openProjectOnboarding={jest.fn()} organizations={organizations} />)
  ).toMatchSnapshot();
});
