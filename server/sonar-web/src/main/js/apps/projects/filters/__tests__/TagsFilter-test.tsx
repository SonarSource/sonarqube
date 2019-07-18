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
import { shallow } from 'enzyme';
import * as React from 'react';
import TagsFilter from '../TagsFilter';

const tags = ['lang', 'sonar', 'csharp', 'dotnet', 'it', 'net'];
const tagsFacet = { lang: 4, sonar: 3, csharp: 1 };

it('should render the tags without the ones in the facet', () => {
  const wrapper = shallow(
    <TagsFilter facet={tagsFacet} onQueryChange={jest.fn()} query={{ tags: null }} />
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ tags });
  expect(wrapper).toMatchSnapshot();
});

it('should render the tags facet with the selected tags', () => {
  const wrapper = shallow(
    <TagsFilter
      facet={tagsFacet}
      onQueryChange={jest.fn()}
      query={{ tags: ['lang', 'sonar'] }}
      value={['lang', 'sonar']}
    />
  );
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('Filter').shallow()).toMatchSnapshot();
});

it('should render maximum 10 tags in the searchbox results', () => {
  const wrapper = shallow(
    <TagsFilter
      facet={{ ...tagsFacet, ad: 1 }}
      onQueryChange={jest.fn()}
      query={{ languages: ['java', 'ad'] }}
      value={['java', 'ad']}
    />
  );
  wrapper.setState({ tags: [...tags, 'aa', 'ab', 'ac', 'ad', 'ae', 'af', 'ag', 'ah', 'ai'] });
  expect(wrapper).toMatchSnapshot();
});
