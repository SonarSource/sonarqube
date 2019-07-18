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
import DocMarkdownBlock from '../DocMarkdownBlock';

const CONTENT = `
## Lorem ipsum

Quisque vitae tincidunt felis. Nam blandit risus placerat, efficitur enim ut, pellentesque sem. Mauris non lorem auctor, consequat neque eget, dignissim augue.

## Sit amet

### Maecenas diam

Velit, vestibulum nec ultrices id, mollis eget arcu. Sed dapibus, sapien ut auctor consectetur, mi tortor vestibulum ante, eget dapibus lacus risus.

### Integer

At cursus turpis. Aenean at elit fringilla, porttitor mi eget, dapibus nisi. Donec quis congue odio.

## Nam blandit 

Risus placerat, efficitur enim ut, pellentesque sem. Mauris non lorem auctor, consequat neque eget, dignissim augue.
`;

// mock `remark` & co to work around the issue with cjs imports
jest.mock('remark', () => {
  const remark = require.requireActual('remark');
  return { default: remark };
});

jest.mock('remark-react', () => {
  const remarkReact = require.requireActual('remark-react');
  return { default: remarkReact };
});

jest.mock('remark-slug', () => {
  const remarkSlug = require.requireActual('remark-slug');
  return { default: remarkSlug };
});

jest.mock('../../../helpers/system', () => ({
  getInstance: jest.fn(),
  isSonarCloud: jest.fn()
}));

it('should render simple markdown', () => {
  expect(shallowRender({ content: 'this is *bold* text' })).toMatchSnapshot();
});

it('should use custom component for links', () => {
  expect(
    shallowRender({ content: 'some [link](/quality-profiles)' }).find('withChildProps')
  ).toMatchSnapshot();
});

it('should render with custom props for links', () => {
  expect(
    shallowRender({
      childProps: { foo: 'bar' },
      content: 'some [link](#quality-profiles)',
      isTooltip: true
    }).find('withChildProps')
  ).toMatchSnapshot();
});

it('should render a sticky TOC if available', () => {
  const wrapper = shallowRender({ content: CONTENT, stickyToc: true });
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('DocToc').exists()).toBe(true);
});

function shallowRender(props: Partial<DocMarkdownBlock['props']> = {}) {
  return shallow(<DocMarkdownBlock content="" {...props} />);
}
