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
import { KeyboardKeys } from '../../../../helpers/keycodes';
import { mockMetric } from '../../../../helpers/testMocks';
import { keydown } from '../../../../helpers/testUtils';
import FilesView from '../FilesView';

const COMPONENTS = [
  {
    key: 'foo',
    measures: [],
    name: 'Foo',
    qualifier: 'TRK',
  },
];

const METRICS = { coverage: { id: '1', key: 'coverage', type: 'PERCENT', name: 'Coverage' } };

it('should renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render with best values hidden', () => {
  expect(
    shallowRender({
      components: [
        ...COMPONENTS,
        {
          key: 'bar',
          measures: [{ bestValue: true, metric: mockMetric({ key: 'coverage' }) }],
          name: 'Bar',
          qualifier: 'TRK',
        },
      ],
    })
  ).toMatchSnapshot();
});

it('should correctly bind key events for file navigation', () => {
  const handleSelect = jest.fn();
  const handleOpen = jest.fn();
  const FILES = [
    {
      key: 'foo',
      measures: [],
      name: 'Foo',
      qualifier: 'TRK',
    },
    {
      key: 'bar',
      measures: [],
      name: 'Bar',
      qualifier: 'TRK',
    },
    {
      key: 'yoo',
      measures: [],
      name: 'Yoo',
      qualifier: 'TRK',
    },
  ];

  shallowRender({
    handleSelect,
    handleOpen,
    selectedComponent: FILES[0],
    components: FILES,
  });

  keydown({ key: KeyboardKeys.DownArrow });
  expect(handleSelect).toHaveBeenCalledWith(FILES[0]);

  keydown({ key: KeyboardKeys.UpArrow });
  expect(handleSelect).toHaveBeenCalledWith(FILES[2]);

  keydown({ key: KeyboardKeys.RightArrow, ctrlKey: true });
  expect(handleOpen).not.toHaveBeenCalled();

  keydown({ key: KeyboardKeys.RightArrow });
  expect(handleOpen).toHaveBeenCalled();
});

function shallowRender(props: Partial<FilesView['props']> = {}) {
  return shallow<FilesView>(
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
        qualifier: 'TRK',
      }}
      view="tree"
      {...props}
    />
  );
}
