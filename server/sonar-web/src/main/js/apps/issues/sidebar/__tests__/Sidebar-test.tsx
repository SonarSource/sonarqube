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
import { shallow, ShallowWrapper } from 'enzyme';
import { flatten } from 'lodash';
import * as React from 'react';
import { Query } from '../../utils';
import Sidebar, { Props } from '../Sidebar';

jest.mock('../../../../store/rootReducer', () => ({}));

const renderSidebar = (props?: Partial<Props>) => {
  return flatten(
    mapChildren(
      shallow(
        <Sidebar
          component={undefined}
          facets={{}}
          loadSearchResultCount={jest.fn()}
          loadingFacets={{}}
          myIssues={false}
          onFacetToggle={jest.fn()}
          onFilterChange={jest.fn()}
          openFacets={{}}
          organization={undefined}
          query={{ types: [''] } as Query}
          referencedComponentsById={{}}
          referencedComponentsByKey={{}}
          referencedLanguages={{}}
          referencedRules={{}}
          referencedUsers={{}}
          {...props}
        />
      )
    )
  );

  function mapChildren(wrapper: ShallowWrapper) {
    return wrapper.children().map(node => {
      if (typeof node.type() === 'symbol') {
        return node.children().map(node => node.name());
      }
      return node.name();
    });
  }
};

const component = {
  breadcrumbs: [],
  name: 'foo',
  key: 'foo',
  organization: 'org'
};

it('should render facets for global page', () => {
  expect(renderSidebar()).toMatchSnapshot();
});

it('should render facets for project', () => {
  expect(renderSidebar({ component: { ...component, qualifier: 'TRK' } })).toMatchSnapshot();
});

it('should render facets for module', () => {
  expect(renderSidebar({ component: { ...component, qualifier: 'BRC' } })).toMatchSnapshot();
});

it('should render facets for directory', () => {
  expect(renderSidebar({ component: { ...component, qualifier: 'DIR' } })).toMatchSnapshot();
});

it('should render facets for developer', () => {
  expect(renderSidebar({ component: { ...component, qualifier: 'DEV' } })).toMatchSnapshot();
});

it('should render facets when my issues are selected', () => {
  expect(renderSidebar({ myIssues: true })).toMatchSnapshot();
});
