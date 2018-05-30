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
import EditionBoxes from '../EditionBoxes';

jest.mock('../utils', () => ({
  EDITIONS: [
    { key: 'community', name: 'Community Edition', homeUrl: 'more_url' },
    {
      key: 'developer',
      name: 'Developer Edition',
      homeUrl: 'more_url'
    },
    {
      key: 'enterprise',
      name: 'Enterprise Edition',
      homeUrl: 'more_url'
    },
    {
      key: 'datacenter',
      name: 'Data Center Edition',
      homeUrl: 'more_url'
    }
  ]
}));

it('should display the available edition boxes correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should display the enterprise and datacenter edition boxes', () => {
  expect(getWrapper({ currentEdition: 'developer' })).toMatchSnapshot();
});

it('should display the datacenter edition box only', () => {
  expect(getWrapper({ currentEdition: 'enterprise' })).toMatchSnapshot();
});

it('should not display any edition box', () => {
  expect(getWrapper({ currentEdition: 'datacenter' }).type()).toBeNull();
});

function getWrapper(props = {}) {
  return shallow(<EditionBoxes currentEdition={'community'} {...props} />);
}
