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
import CodeSnippet, { CodeSnippetProps } from '../CodeSnippet';

beforeEach(jest.clearAllMocks);

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ noCopy: true })).toMatchSnapshot('no copy');
  expect(shallowRender({ snippet: ['foo', 'bar'] })).toMatchSnapshot('array snippet');
  expect(shallowRender({ isOneLine: true, snippet: ['foo', 'bar'] })).toMatchSnapshot(
    'single line with array snippet'
  );
});

function shallowRender(props: Partial<CodeSnippetProps> = {}) {
  return shallow<CodeSnippetProps>(<CodeSnippet snippet={'foo\nbar'} {...props} />);
}
