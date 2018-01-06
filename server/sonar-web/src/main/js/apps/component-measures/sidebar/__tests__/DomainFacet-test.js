/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import { shallow } from 'enzyme';
import DomainFacet from '../DomainFacet';

const DOMAIN = {
  name: 'Reliability',
  measures: [
    {
      metric: {
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
        key: 'new_bugs',
        type: 'INT',
        name: 'New Bugs',
        domain: 'Reliability'
      },
      periods: [{ index: 1, value: '5' }],
      leak: '5'
    }
  ]
};

const PROPS = {
  onChange: () => {},
  onToggle: () => {},
  open: true,
  domain: DOMAIN,
  selected: 'foo'
};

it('should display facet item list', () => {
  expect(shallow(<DomainFacet {...PROPS} />)).toMatchSnapshot();
});

it('should display facet item list with bugs selected', () => {
  expect(shallow(<DomainFacet {...PROPS} selected="bugs" />)).toMatchSnapshot();
});

it('should render closed', () => {
  const wrapper = shallow(<DomainFacet {...PROPS} open={false} />);
  expect(wrapper.find('FacetItemsList')).toHaveLength(0);
});
