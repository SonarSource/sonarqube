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
import DomainFacet from '../DomainFacet';

it('should display facet item list', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should display facet item list with bugs selected', () => {
  expect(shallowRender({ selected: 'bugs' })).toMatchSnapshot();
});

it('should render closed', () => {
  const wrapper = shallowRender({ open: false });
  expect(wrapper.find('FacetItemsList')).toHaveLength(0);
});

it('should render without overview', () => {
  const wrapper = shallowRender({ showFullMeasures: false });
  expect(
    wrapper
      .find('FacetItem')
      .filterWhere(node => node.getElement().key === 'Reliability')
      .exists()
  ).toBe(false);
});

it('should not display subtitles of new measures if there is none', () => {
  const domain = {
    name: 'Reliability',
    measures: [
      {
        metric: { id: '1', key: 'bugs', type: 'INT', name: 'Bugs', domain: 'Reliability' },
        value: '5'
      }
    ]
  };

  expect(shallowRender({ domain })).toMatchSnapshot();
});

it('should not display subtitles of new measures if there is none, even on last line', () => {
  const domain = {
    name: 'Reliability',
    measures: [
      {
        metric: { id: '2', key: 'new_bugs', type: 'INT', name: 'New Bugs', domain: 'Reliability' },
        value: '5'
      }
    ]
  };

  expect(shallowRender({ domain })).toMatchSnapshot();
});

function shallowRender(props: Partial<DomainFacet['props']> = {}) {
  return shallow(
    <DomainFacet
      domain={{
        name: 'Reliability',
        measures: [
          {
            metric: {
              id: '1',
              key: 'bugs',
              type: 'INT',
              name: 'Bugs',
              domain: 'Reliability'
            },
            value: '5',
            periods: [{ index: 1, value: '5' }],
            leak: '5'
          },
          {
            metric: {
              id: '2',
              key: 'new_bugs',
              type: 'INT',
              name: 'New Bugs',
              domain: 'Reliability'
            },
            periods: [{ index: 1, value: '5' }],
            leak: '5'
          }
        ]
      }}
      onChange={() => {}}
      onToggle={() => {}}
      open={true}
      selected={'foo'}
      showFullMeasures={true}
      {...props}
    />
  );
}
