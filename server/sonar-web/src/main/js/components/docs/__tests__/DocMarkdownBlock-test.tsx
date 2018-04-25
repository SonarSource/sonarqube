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
import * as React from 'react';
import { shallow } from 'enzyme';
import DocMarkdownBlock from '../DocMarkdownBlock';

// mock `remark` and `remark-react` to work around the issue with cjs imports
jest.mock('remark', () => {
  const remark = require.requireActual('remark');
  return { default: remark };
});

jest.mock('remark-react', () => {
  const remarkReact = require.requireActual('remark-react');
  return { default: remarkReact };
});

it('should render simple markdown', () => {
  expect(shallow(<DocMarkdownBlock content="this is *bold* text" />)).toMatchSnapshot();
});

it('should render use custom component for links', () => {
  expect(
    shallow(<DocMarkdownBlock content="some [link](#quality-profiles)" />).find('DocLink')
  ).toMatchSnapshot();
});
