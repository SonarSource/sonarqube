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
import ComponentsList from '../ComponentsList';

const COMPONENTS = [
  {
    key: 'foo',
    measures: [],
    name: 'Foo',
    organization: 'foo',
    qualifier: 'TRK'
  }
];

const METRICS = {
  coverage: { id: '1', key: 'coverage', type: 'PERCENT', name: 'Coverage' },
  new_bugs: { id: '2', key: 'new_bugs', type: 'INT', name: 'New Bugs' },
  uncovered_lines: { id: '3', key: 'uncovered_lines', type: 'INT', name: 'Lines' },
  uncovered_conditions: { id: '4', key: 'uncovered_conditions', type: 'INT', name: 'Conditions' }
};

it('should renders correctly', () => {
  expect(
    shallow(
      <ComponentsList
        components={COMPONENTS}
        metric={METRICS.new_bugs}
        metrics={METRICS}
        onClick={jest.fn()}
        rootComponent={COMPONENTS[0]}
        view="tree"
      />
    )
  ).toMatchSnapshot();
});

it('should renders empty', () => {
  expect(
    shallow(
      <ComponentsList
        components={[]}
        metric={METRICS.new_bugs}
        metrics={METRICS}
        onClick={jest.fn()}
        rootComponent={COMPONENTS[0]}
        view="tree"
      />
    )
  ).toMatchSnapshot();
});

it('should renders with multiple measures', () => {
  expect(
    shallow(
      <ComponentsList
        components={COMPONENTS}
        metric={METRICS.coverage}
        metrics={METRICS}
        onClick={jest.fn()}
        rootComponent={COMPONENTS[0]}
        view="tree"
      />
    )
  ).toMatchSnapshot();
});
