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
import FilesView from '../FilesView';

const COMPONENTS = [
  {
    key: 'foo',
    measures: [],
    name: 'Foo',
    organization: 'foo',
    qualifier: 'TRK'
  }
];

const METRICS = { coverage: { id: '1', key: 'coverage', type: 'PERCENT', name: 'Coverage' } };

it('should renders correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should render with best values hidden', () => {
  expect(
    getWrapper({
      components: [
        ...COMPONENTS,
        {
          key: 'bar',
          measures: [{ bestValue: true, metric: { key: 'coverage' } }],
          name: 'Bar',
          organization: 'foo',
          qualifier: 'TRK'
        }
      ]
    })
  ).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <FilesView
      components={COMPONENTS}
      defaultShowBestMeasures={false}
      fetchMore={jest.fn()}
      handleOpen={jest.fn()}
      handleSelect={jest.fn()}
      loadingMore={false}
      metric={METRICS.coverage}
      metrics={METRICS}
      paging={{ pageIndex: 0, pageSize: 5, total: 10 }}
      rootComponent={{
        key: 'parent',
        measures: [],
        name: 'Parent',
        organization: 'foo',
        qualifier: 'TRK'
      }}
      view="tree"
      {...props}
    />
  );
}
