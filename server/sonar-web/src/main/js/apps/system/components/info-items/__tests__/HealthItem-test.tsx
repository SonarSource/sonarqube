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
import HealthItem from '../HealthItem';
import { HealthType } from '../../../../../api/system';

it('should render correctly', () => {
  expect(
    shallow(
      <HealthItem biggerHealth={true} health={HealthType.RED} healthCauses={['foo']} name="Foo" />
    )
  ).toMatchSnapshot();
});

it('should not render health causes', () => {
  expect(
    shallow(<HealthItem health={HealthType.GREEN} healthCauses={['foo']} />)
  ).toMatchSnapshot();
  expect(shallow(<HealthItem health={HealthType.YELLOW} healthCauses={[]} />)).toMatchSnapshot();
});

it('should render multiple health causes', () => {
  expect(
    shallow(<HealthItem health={HealthType.YELLOW} healthCauses={['foo', 'bar']} />)
  ).toMatchSnapshot();
});
