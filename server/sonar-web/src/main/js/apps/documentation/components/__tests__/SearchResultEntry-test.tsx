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
import SearchResultEntry, {
  SearchResult,
  SearchResultText,
  SearchResultTitle,
  SearchResultTokens
} from '../SearchResultEntry';

describe('SearchResultEntry', () => {
  it('should render', () => {
    expect(
      shallow(<SearchResultEntry active={true} result={mockSearchResult()} />)
    ).toMatchSnapshot();
  });
});

describe('SearchResultText', () => {
  it('should render with highlights', () => {
    expect(
      shallow(<SearchResultText result={mockSearchResult({ highlights: { text: [[12, 9]] } })} />)
    ).toMatchSnapshot();
  });

  it('should correctly extract exact matches', () => {
    expect(
      shallow(
        <SearchResultText
          result={mockSearchResult({ exactMatch: true, query: 'variable understood' })}
        />
      )
    ).toMatchSnapshot();
  });

  it('should render without highlights', () => {
    expect(shallow(<SearchResultText result={mockSearchResult()} />)).toMatchSnapshot();
  });
});

describe('SearchResultTitle', () => {
  it('should render with highlights', () => {
    expect(
      shallow(<SearchResultTitle result={mockSearchResult({ highlights: { title: [[0, 6]] } })} />)
    ).toMatchSnapshot();
  });

  it('should render not without highlights', () => {
    expect(shallow(<SearchResultTitle result={mockSearchResult()} />)).toMatchSnapshot();
  });
});

describe('SearchResultTokens', () => {
  it('should render', () => {
    expect(
      shallow(
        <SearchResultTokens
          tokens={[
            { marked: false, text: 'Foobar is a ' },
            { marked: true, text: 'universal' },
            {
              marked: false,
              text: ' variable understood to represent whatever is being discussed.'
            }
          ]}
        />
      )
    ).toMatchSnapshot();
  });
});

function mockSearchResult(overrides: Partial<SearchResult> = {}) {
  return {
    page: {
      content: '',
      relativeName: 'foo/bar',
      url: '/foo/bar',
      text: 'Foobar is a universal variable understood to represent whatever is being discussed.',
      title: 'Foobar',
      navTitle: undefined
    },
    highlights: {},
    longestTerm: '',
    query: '',
    ...overrides
  };
}
