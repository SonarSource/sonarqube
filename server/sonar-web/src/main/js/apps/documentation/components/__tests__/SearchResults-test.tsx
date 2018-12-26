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
import lunr from 'lunr';
import SearchResults from '../SearchResults';

jest.mock('lunr', () => ({
  default: jest.fn(() => ({
    search: jest.fn(() => [
      {
        ref: 'lorem/origin',
        matchData: {
          metadata: {
            simply: {
              title: { position: [[19, 5]] },
              text: {
                position: [[15, 6], [28, 4]],
                tokenContext: ['is simply dummy', 'simply dummy text']
              }
            }
          }
        }
      },
      {
        ref: 'foobar',
        matchData: {
          metadata: {
            simply: {
              title: { position: [[23, 4]] },
              text: { position: [[111, 6], [118, 4]], tokenContext: ['keywords simply text'] }
            }
          }
        }
      }
    ])
  }))
}));

function createPage(title: string, relativeName: string, text = '') {
  return { relativeName, url: '/' + relativeName, title, navTitle: undefined, text, content: text };
}

const pages = [
  createPage(
    'Lorem Ipsum',
    'lorem/index',
    "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book."
  ),
  createPage(
    'Where does it come from?',
    'lorem/origin',
    'Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words.'
  ),
  createPage(
    'Where does Foobar come from?',
    'foobar',
    'Foobar is a universal variable understood to represent whatever is being discussed. Now we need some keywords: simply text.'
  )
];

it('should search', () => {
  const wrapper = shallow(
    <SearchResults
      navigation={['lorem/index', 'lorem/origin', 'foobar']}
      pages={pages}
      query="simply text"
      splat="foobar"
    />
  );
  expect(wrapper).toMatchSnapshot();
  expect(lunr).toBeCalled();
  expect((wrapper.instance() as SearchResults).index.search).toBeCalledWith(
    'simply~1 simply* text~1 text*'
  );
});
