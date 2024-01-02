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
import ListStyleFacet from '../../../../components/facet/ListStyleFacet';
import { mockComponent } from '../../../../helpers/mocks/component';
import { Query } from '../../utils';
import AuthorFacet from '../AuthorFacet';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should notify of search result count correctly', () => {
  const loadSearchResultCount = jest.fn();

  const wrapper = shallowRender({ loadSearchResultCount });

  wrapper.find(ListStyleFacet).props().loadSearchResultCount!(['1', '2']);

  expect(loadSearchResultCount).toHaveBeenCalled();
});

function shallowRender(props: Partial<AuthorFacet['props']> = {}) {
  return shallow<AuthorFacet>(
    <AuthorFacet
      component={mockComponent()}
      fetching={false}
      loadSearchResultCount={jest.fn()}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={true}
      query={{} as Query}
      stats={{}}
      author={[]}
      {...props}
    />
  );
}
