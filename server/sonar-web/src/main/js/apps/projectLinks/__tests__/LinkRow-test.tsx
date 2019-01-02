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
import { shallow } from 'enzyme';
import LinkRow from '../LinkRow';

it('should render provided link', () => {
  expect(
    shallow(
      <LinkRow
        link={{ id: '12', type: 'homepage', url: 'http://example.com' }}
        onDelete={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});

it('should render custom link', () => {
  expect(
    shallow(
      <LinkRow
        link={{ id: '12', name: 'foo', type: 'foo', url: 'http://example.com' }}
        onDelete={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});

it('should render dangerous code as plain text', () => {
  expect(
    shallow(
      <LinkRow
        link={{ id: '12', name: 'dangerous', type: 'dangerous', url: 'javascript:alert("Hello")' }}
        onDelete={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});
