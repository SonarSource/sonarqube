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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import MetaData from 'sonar-ui-common/components/ui/update-center/MetaData';
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
jest.mock('remark', () => ({ default: jest.requireActual('remark') }));
jest.mock('remark-rehype', () => ({ default: jest.requireActual('remark-rehype') }));
jest.mock('rehype-raw', () => ({ default: jest.requireActual('rehype-raw') }));
jest.mock('rehype-react', () => ({ default: jest.requireActual('rehype-react') }));
jest.mock('remark-slug', () => ({ default: jest.requireActual('remark-slug') }));

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

it('should correctly render update-center tags', () => {
  const wrapper = mount(
    <DocMarkdownBlock content='<update-center updatecenterkey="abap"></update-center>' />
  );
  expect(wrapper.find(MetaData).length).toBe(1);
});

function shallowRender(props: Partial<DocMarkdownBlock['props']> = {}) {
  return shallow(<DocMarkdownBlock content="" {...props} />);
}
