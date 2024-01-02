/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { mockComponent } from '../../../../../helpers/mocks/component';
import { OSs } from '../../../types';
import ExecScanner, { ExecScannerProps } from '../ExecScanner';

it.each([OSs.Linux, OSs.Windows, OSs.MacOS])('should render correctly for %p', (os) => {
  expect(shallowRender({ os })).toMatchSnapshot();
});

it('should render correctly for cfamily', () => {
  expect(shallowRender({ cfamily: true })).toMatchSnapshot();
});

it('should render correctly for remote execution', () => {
  expect(shallowRender({ isLocal: false })).toMatchSnapshot();
});

function shallowRender(props: Partial<ExecScannerProps> = {}) {
  return shallow<ExecScannerProps>(
    <ExecScanner
      baseUrl="host"
      isLocal={true}
      os={OSs.Linux}
      component={mockComponent({ key: 'projectKey' })}
      token="token"
      {...props}
    />
  );
}
