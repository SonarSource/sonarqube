/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import LanguagesFilter from '../LanguagesFilter';

const languages = {
  java: {
    key: 'java',
    name: 'Java'
  },
  cs: {
    key: 'cs',
    name: 'C#'
  },
  js: {
    key: 'js',
    name: 'JavaScript'
  },
  flex: {
    key: 'flex',
    name: 'Flex'
  },
  php: {
    key: 'php',
    name: 'PHP'
  },
  py: {
    key: 'py',
    name: 'Python'
  }
};
const languagesFacet = { java: 39, cs: 4, js: 1 };
const fakeRouter = { push: () => {} };

it('should render the languages without the ones in the facet', () => {
  const wrapper = shallow(
    <LanguagesFilter
      query={{ languages: null }}
      languages={languages}
      router={fakeRouter}
      facet={languagesFacet}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should render the languages facet with the selected languages', () => {
  const wrapper = shallow(
    <LanguagesFilter
      query={{ languages: ['java', 'cs'] }}
      value={['java', 'cs']}
      languages={languages}
      router={fakeRouter}
      facet={languagesFacet}
      isFavorite={true}
    />
  );
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('Filter').shallow()).toMatchSnapshot();
});

it('should render maximum 10 languages in the searchbox results', () => {
  const wrapper = shallow(
    <LanguagesFilter
      query={{ languages: ['java', 'g'] }}
      value={['java', 'g']}
      languages={{
        ...languages,
        c: { key: 'c', name: 'c' },
        d: { key: 'd', name: 'd' },
        e: { key: 'e', name: 'e' },
        f: { key: 'f', name: 'f' },
        g: { key: 'g', name: 'g' },
        h: { key: 'h', name: 'h' },
        i: { key: 'i', name: 'i' },
        k: { key: 'k', name: 'k' },
        l: { key: 'l', name: 'l' }
      }}
      router={fakeRouter}
      facet={{ ...languagesFacet, g: 1 }}
      isFavorite={true}
    />
  );
  expect(wrapper).toMatchSnapshot();
});
