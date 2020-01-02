/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { FetchMock } from 'jest-fetch-mock';
import * as React from 'react';
import { MarkdownRemark } from '../../@types/graphql-types';
import Sidebar from '../Sidebar';

jest.mock('../navTreeUtils', () => {
  return {
    ...require.requireActual('../navTreeUtils'),
    getNavTree: jest.fn().mockReturnValue([
      '/foo/',
      {
        title: 'Foo subs',
        children: [
          '/foo/bar/',
          '/foo/baz/',
          {
            title: 'Foo Baz subs',
            children: [
              '/foo/baz/bar/',
              '/foo/baz/foo/',
              {
                title: 'Foo Baz Foo subs',
                children: ['/foo/baz/foo/bar/', '/foo/baz/foo/baz']
              }
            ]
          }
        ]
      },
      '/bar/',
      {
        title: 'Bar subs',
        children: [{ title: 'External link 1', url: 'http://example.com/1' }, '/bar/foo/']
      },
      { title: 'External link 2', url: 'http://example.com/2' }
    ])
  };
});

beforeEach(() => {
  (fetch as FetchMock).resetMocks();
  (fetch as FetchMock).mockResponse(`[
    { "value": "3.0", "current": true },
    { "value": "2.0", "current": false, "lts": true },
    { "value": "1.0", "current": false }
  ]`);
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await new Promise(setImmediate);

  expect(wrapper).toMatchSnapshot('default');
  expect(wrapper.setProps({ version: '1.0' })).toMatchSnapshot('show warning');
  expect(wrapper.setProps({ version: '2.0' })).toMatchSnapshot('lts');
});

function shallowRender(props: Partial<Sidebar['props']> = {}) {
  return shallow<Sidebar>(
    <Sidebar
      location={{ pathname: '/2.0/foo/baz/foo/bar' } as Location}
      pages={[
        {
          fields: {
            slug: '/foo/'
          },
          frontmatter: {
            title: 'Foo'
          }
        } as MarkdownRemark,
        {
          fields: {
            slug: '/foo/baz/bar'
          },
          frontmatter: {
            title: 'Foo Baz Bar'
          }
        } as MarkdownRemark,
        {
          fields: {
            slug: '/bar/'
          },
          frontmatter: {
            title: 'Bar'
          }
        } as MarkdownRemark
      ]}
      version="3.0"
      {...props}
    />
  );
}
