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
import ComponentsHeader from '../ComponentsHeader';
import { mockComponent } from '../../../../helpers/testMocks';

it('renders correctly for projects', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders correctly for portfolios', () => {
  const portfolio = mockComponent({ qualifier: 'VW' });
  expect(shallowRender({ baseComponent: portfolio, rootComponent: portfolio })).toMatchSnapshot();
});

it('renders correctly for a search', () => {
  expect(shallowRender({ baseComponent: undefined })).toMatchSnapshot();
});

function shallowRender(props = {}) {
  return shallow(
    <ComponentsHeader
      baseComponent={mockComponent()}
      metrics={['foo', 'bar']}
      rootComponent={mockComponent()}
      {...props}
    />
  );
}
