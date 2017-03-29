/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import Params from '../Params';

it('should render deprecated parameters', () => {
  const params = [
    {
      key: 'foo',
      deprecatedSince: '5.0'
    }
  ];
  expect(shallow(<Params params={params} showDeprecated={true} />)).toMatchSnapshot();
});

it('should not render deprecated parameters', () => {
  const params = [
    {
      key: 'foo',
      deprecatedSince: '5.0'
    }
  ];
  expect(shallow(<Params params={params} showDeprecated={false} />)).toMatchSnapshot();
});

it('should render deprecated key', () => {
  const params = [
    {
      key: 'foo',
      deprecatedKey: 'foo-deprecated',
      deprecatedKeySince: '5.0'
    }
  ];
  expect(shallow(<Params params={params} showDeprecated={true} />)).toMatchSnapshot();
});
