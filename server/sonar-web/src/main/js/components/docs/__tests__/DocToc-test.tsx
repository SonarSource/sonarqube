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
import { mount } from 'enzyme';
import * as React from 'react';
import { click, scrollTo } from 'sonar-ui-common/helpers/testUtils';
import DocToc from '../DocToc';

const OFFSET = 300;

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

jest.mock('remark', () => {
  const remark = require.requireActual('remark');
  return { default: remark };
});

jest.mock('remark-react', () => {
  const remarkReact = require.requireActual('remark-react');
  return { default: remarkReact };
});

jest.mock('lodash', () => {
  const lodash = require.requireActual('lodash');
  lodash.debounce = (fn: any) => fn;
  return lodash;
});

jest.mock('react-dom', () => ({
  findDOMNode: jest.fn()
}));

it('should render correctly', () => {
  const wrapper = renderComponent();
  expect(wrapper).toMatchSnapshot();
});

it('should trigger the handler when an anchor is clicked', () => {
  const onAnchorClick = jest.fn();
  const wrapper = renderComponent({ onAnchorClick });
  click(wrapper.find('a[href="#sit-amet"]'));
  expect(onAnchorClick).toBeCalled();
});

it('should highlight anchors when scrolling', () => {
  mockDomEnv();
  const wrapper = renderComponent();

  scrollTo({ top: OFFSET });
  expect(wrapper.state('highlightAnchor')).toEqual('#lorem-ipsum');

  scrollTo({ top: OFFSET * 3 });
  expect(wrapper.state('highlightAnchor')).toEqual('#nam-blandit');
});

function renderComponent(props: Partial<DocToc['props']> = {}) {
  return mount(<DocToc content={CONTENT} onAnchorClick={jest.fn()} {...props} />);
}

function mockDomEnv() {
  const findDOMNode = require('react-dom').findDOMNode as jest.Mock<any>;
  const parent = document.createElement('div');
  const element = document.createElement('div');
  parent.appendChild(element);

  let offset = OFFSET;
  (CONTENT.match(/^## .+$/gm) as Array<string>).forEach(match => {
    const slug = match
      .replace(/^#+ */, '')
      .replace(' ', '-')
      .toLowerCase()
      .trim();
    const heading = document.createElement('h2');
    heading.id = slug;
    Object.defineProperty(heading, 'offsetTop', { value: offset });
    offset += OFFSET;

    parent.appendChild(heading);
  });

  findDOMNode.mockReturnValue(element);
}
