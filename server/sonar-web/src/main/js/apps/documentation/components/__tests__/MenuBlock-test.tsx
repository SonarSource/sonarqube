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
import MenuBlock from '../MenuBlock';

const block = {
  title: 'Foo',
  children: ['/bar/', '/baz/']
};

const pages = [
  {
    content: 'bar',
    relativeName: '/bar/',
    text: 'bar',
    title: 'Bar',
    navTitle: undefined,
    url: '/bar/'
  },
  {
    content: 'baz',
    relativeName: '/baz/',
    text: 'baz',
    title: 'baz',
    navTitle: 'baznav',
    url: '/baz/'
  }
];

it('should render a closed menu block', () => {
  expect(
    shallow(
      <MenuBlock
        block={block}
        onToggle={jest.fn()}
        open={false}
        pages={pages}
        splat="/foobar/"
        title="Foobarbaz"
      />
    )
  ).toMatchSnapshot();
});

it('should render an opened menu block', () => {
  expect(
    shallow(
      <MenuBlock
        block={block}
        onToggle={jest.fn()}
        open={true}
        pages={pages}
        splat="/foo/"
        title="Foo"
      />
    )
  ).toMatchSnapshot();
});
