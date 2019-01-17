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
import { Components } from '../Components';

const COMPONENT = { key: 'foo', name: 'Foo', qualifier: 'TRK' };
const PORTFOLIO = { key: 'bar', name: 'Bar', qualifier: 'VW' };
const METRICS = { coverage: { id: '1', key: 'coverage', type: 'PERCENT', name: 'Coverage' } };
const BRANCH = {
  isMain: false,
  name: 'feature',
  mergeBranch: 'master',
  type: 'SHORT'
};

it('renders correctly', () => {
  expect(
    shallow(
      <Components
        baseComponent={COMPONENT}
        components={[COMPONENT]}
        metrics={METRICS}
        rootComponent={COMPONENT}
      />
    )
  ).toMatchSnapshot();
});

it('renders correctly for a search', () => {
  expect(
    shallow(<Components components={[COMPONENT]} metrics={METRICS} rootComponent={COMPONENT} />)
  ).toMatchSnapshot();
});

it('renders correctly for leak', () => {
  expect(
    shallow(
      <Components
        baseComponent={COMPONENT}
        branchLike={BRANCH}
        components={[COMPONENT]}
        metrics={METRICS}
        rootComponent={COMPONENT}
      />
    )
  ).toMatchSnapshot();
});

it('handle no components correctly', () => {
  expect(
    shallow(
      <Components
        baseComponent={PORTFOLIO}
        components={[]}
        metrics={METRICS}
        rootComponent={PORTFOLIO}
      />
    )
      .find('ComponentsEmpty')
      .exists()
  ).toBe(true);
});
