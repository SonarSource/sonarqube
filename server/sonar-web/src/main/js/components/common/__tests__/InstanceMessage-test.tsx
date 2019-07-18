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
import { getInstance } from '../../../helpers/system';
import InstanceMessage from '../InstanceMessage';

jest.mock('../../../helpers/system', () => ({ getInstance: jest.fn() }));

it('should replace {instance} with "SonarQube"', () => {
  const childFunc = jest.fn();
  shallowRender(childFunc, 'foo {instance} bar');
  expect(childFunc).toHaveBeenCalledWith('foo SonarQube bar');
});

it('should replace {instance} with "SonarCloud"', () => {
  const childFunc = jest.fn();
  shallowRender(childFunc, 'foo {instance} bar', true);
  expect(childFunc).toHaveBeenCalledWith('foo SonarCloud bar');
});

it('should return the same message', () => {
  const childFunc = jest.fn();
  shallowRender(childFunc, 'no instance to replace');
  expect(childFunc).toHaveBeenCalledWith('no instance to replace');
});

function shallowRender(
  children: (msg: string) => React.ReactChild,
  message: string,
  onSonarCloud = false
) {
  (getInstance as jest.Mock).mockImplementation(() => (onSonarCloud ? 'SonarCloud' : 'SonarQube'));
  return shallow(<InstanceMessage message={message}>{children}</InstanceMessage>);
}
