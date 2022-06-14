/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { scrollToElement } from '../../../helpers/scrolling';
import { mockEvent } from '../../../helpers/testUtils';
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
jest.mock('remark', () => jest.requireActual('remark'));
jest.mock('remark-rehype', () => jest.requireActual('remark-rehype'));
jest.mock('rehype-raw', () => jest.requireActual('rehype-raw'));
jest.mock('rehype-react', () => jest.requireActual('rehype-react'));
jest.mock('rehype-slug', () => jest.requireActual('rehype-slug'));

jest.mock('../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn()
}));

const WINDOW_HEIGHT = 800;
const originalWindowHeight = window.innerHeight;

const historyPushState = jest.fn();
const originalHistoryPushState = history.pushState;

beforeEach(jest.clearAllMocks);

beforeAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: WINDOW_HEIGHT
  });
  Object.defineProperty(history, 'pushState', {
    writable: true,
    configurable: true,
    value: historyPushState
  });
});

afterAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: originalWindowHeight
  });
  Object.defineProperty(history, 'pushState', {
    writable: true,
    configurable: true,
    value: originalHistoryPushState
  });
});

it('should render correctly', () => {
  expect(shallowRender({ content: 'this is *bold* text' })).toMatchSnapshot('default');
  expect(
    shallowRender({ content: 'some [link](/quality-profiles)' }).find('withChildProps')
  ).toMatchSnapshot('custom component for links');
  expect(
    shallowRender({
      childProps: { foo: 'bar' },
      content: 'some [link](#quality-profiles)',
      isTooltip: true
    }).find('withChildProps')
  ).toMatchSnapshot('custom props for links');
  expect(shallowRender({ content: CONTENT, stickyToc: true })).toMatchSnapshot('sticky TOC');
});

it('should correctly scroll to clicked headings', () => {
  const element = {} as Element;
  const querySelector: (selector: string) => Element | null = jest.fn((selector: string) =>
    selector === '#id' ? element : null
  );
  const preventDefault = jest.fn();
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  // Node Ref isn't set yet.
  instance.handleAnchorClick('#unknown', mockEvent());
  expect(scrollToElement).not.toBeCalled();

  // Set node Ref.
  instance.node = { querySelector } as HTMLElement;

  // Unknown element.
  instance.handleAnchorClick('#unknown', mockEvent());
  expect(scrollToElement).not.toBeCalled();

  // Known element, should scroll.
  instance.handleAnchorClick('#id', mockEvent({ preventDefault }));
  expect(scrollToElement).toBeCalledWith(element, { bottomOffset: 720 });
  expect(preventDefault).toBeCalled();
  expect(historyPushState).toBeCalledWith(null, '', '#id');
});

it('should correctly scroll to a specific heading if passed as a prop', () => {
  jest.useFakeTimers();

  const element = {} as Element;
  const querySelector: (_: string) => Element | null = jest.fn(() => element);
  const wrapper = shallowRender({ scrollToHref: '#id' });
  const instance = wrapper.instance();
  instance.node = { querySelector } as HTMLElement;

  expect(scrollToElement).not.toBeCalled();

  jest.runAllTimers();

  expect(scrollToElement).toBeCalledWith(element, { bottomOffset: 720 });
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

function shallowRender(props: Partial<DocMarkdownBlock['props']> = {}) {
  return shallow<DocMarkdownBlock>(<DocMarkdownBlock content="" {...props} />);
}
