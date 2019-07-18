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
import Params from '../Params';

const DEFAULT_PARAM: T.WebApi.Param = {
  key: 'foo',
  description: 'Foo desc',
  internal: false,
  required: false
};

it('should render deprecated and internal parameters', () => {
  const params = [
    { ...DEFAULT_PARAM, deprecatedSince: '5.0' },
    { ...DEFAULT_PARAM, deprecatedSince: '5.0', internal: true }
  ];
  expect(
    shallow(<Params params={params} showDeprecated={true} showInternal={true} />)
  ).toMatchSnapshot();
});

it('should not render deprecated parameters', () => {
  const params = [{ ...DEFAULT_PARAM, deprecatedSince: '5.0' }];
  expect(
    shallow(<Params params={params} showDeprecated={false} showInternal={false} />)
  ).toMatchSnapshot();
});

it('should render deprecated key', () => {
  const params = [
    { ...DEFAULT_PARAM, deprecatedKey: 'foo-deprecated', deprecatedSince: '5.0' },
    { ...DEFAULT_PARAM, deprecatedSince: '5.0', internal: true }
  ];
  expect(
    shallow(<Params params={params} showDeprecated={true} showInternal={false} />)
  ).toMatchSnapshot();
});

it('should render different value constraints', () => {
  const param: T.WebApi.Param = {
    ...DEFAULT_PARAM,
    defaultValue: 'def',
    exampleValue: 'foo',
    minimumLength: 2,
    maximumLength: 200,
    minimumValue: 1,
    maximumValue: 500,
    maxValuesAllowed: 1000,
    possibleValues: ['foo', 'bar']
  };
  expect(
    shallow(<Params params={[param]} showDeprecated={true} showInternal={true} />)
  ).toMatchSnapshot();
});
