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
import SearchResults from '../SearchResults';

it('renders different components and dividers between them', () => {
  expect(
    shallow(
      <SearchResults
        allowMore={true}
        loadingMore={null}
        more={{}}
        onMoreClick={jest.fn()}
        onSelect={jest.fn()}
        renderNoResults={() => <div />}
        renderResult={component => <span key={component.key}>{component.name}</span>}
        results={{
          TRK: [component('foo'), component('bar')],
          BRC: [component('qwe', 'BRC'), component('qux', 'BRC')],
          FIL: [component('zux', 'FIL')]
        }}
        selected={null}
      />
    )
  ).toMatchSnapshot();
});

it('renders "Show More" link', () => {
  expect(
    shallow(
      <SearchResults
        allowMore={true}
        loadingMore={null}
        more={{ TRK: 175, BRC: 0 }}
        onMoreClick={jest.fn()}
        onSelect={jest.fn()}
        renderNoResults={() => <div />}
        renderResult={component => <span key={component.key}>{component.name}</span>}
        results={{
          TRK: [component('foo'), component('bar')],
          BRC: [component('qwe', 'BRC'), component('qux', 'BRC')]
        }}
        selected={null}
      />
    )
  ).toMatchSnapshot();
});

function component(key /*: string */, qualifier /*: string */ = 'TRK') {
  return { key, name: key, qualifier };
}
