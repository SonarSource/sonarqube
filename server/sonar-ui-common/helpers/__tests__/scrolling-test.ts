/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { scrollHorizontally, scrollToElement } from '../scrolling';

jest.useFakeTimers();

describe('scrollToElement', () => {
  it('should scroll parent up to element', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ top: 5, bottom: 20 });

    const parent = document.createElement('div');
    parent.getBoundingClientRect = mockGetBoundingClientRect({ height: 30, top: 15 });
    parent.scrollTop = 10;
    parent.scrollLeft = 12;
    parent.appendChild(element);

    document.body.appendChild(parent);
    scrollToElement(element, { parent, smooth: false });

    expect(parent.scrollTop).toEqual(0);
    expect(parent.scrollLeft).toEqual(12);
  });

  it('should scroll parent down to element', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ top: 25, bottom: 50 });

    const parent = document.createElement('div');
    parent.getBoundingClientRect = mockGetBoundingClientRect({ height: 30, top: 15 });
    parent.scrollTop = 10;
    parent.scrollLeft = 12;
    parent.appendChild(element);

    document.body.appendChild(parent);
    scrollToElement(element, { parent, smooth: false });

    expect(parent.scrollTop).toEqual(15);
    expect(parent.scrollLeft).toEqual(12);
  });

  it('should scroll window down to element', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ top: 840, bottom: 845 });

    Object.defineProperty(window, 'innerHeight', { value: 400 });
    window.scrollTo = jest.fn();

    document.body.appendChild(element);

    scrollToElement(element, { smooth: false });

    expect(window.scrollTo).toBeCalledWith(0, 445);
  });

  it('should scroll window up to element', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ top: -10, bottom: 10 });

    Object.defineProperty(window, 'innerHeight', { value: 50 });
    window.scrollTo = jest.fn();

    document.body.appendChild(element);

    scrollToElement(element, { smooth: false });

    expect(window.scrollTo).toBeCalledWith(0, -10);
  });

  it('should scroll window down to element smoothly', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ top: 840, bottom: 845 });

    Object.defineProperty(window, 'innerHeight', { value: 400 });
    window.scrollTo = jest.fn();

    document.body.appendChild(element);

    scrollToElement(element, {});

    jest.runAllTimers();

    expect(window.scrollTo).toBeCalledTimes(10);
  });
});

describe('scrollHorizontally', () => {
  it('should scroll parent left to element', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ left: 25, right: 42 });

    const parent = document.createElement('div');
    parent.getBoundingClientRect = mockGetBoundingClientRect({ width: 67, left: 46 });
    parent.scrollTop = 12;
    parent.scrollLeft = 38;
    parent.appendChild(element);

    document.body.appendChild(parent);

    scrollHorizontally(element, { parent, smooth: false });

    expect(parent.scrollTop).toEqual(12);
    expect(parent.scrollLeft).toEqual(17);
  });

  it('should scroll parent right to element', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ left: 25, right: 99 });

    const parent = document.createElement('div');
    parent.getBoundingClientRect = mockGetBoundingClientRect({ width: 67, left: 20 });
    parent.scrollTop = 12;
    parent.scrollLeft = 20;
    parent.appendChild(element);

    document.body.appendChild(parent);

    scrollHorizontally(element, { parent, smooth: false });

    expect(parent.scrollTop).toEqual(12);
    expect(parent.scrollLeft).toEqual(32);
  });

  it('should scroll window right to element', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ left: 840, right: 845 });

    Object.defineProperty(window, 'innerWidth', { value: 400 });
    window.scrollTo = jest.fn();

    document.body.appendChild(element);

    scrollHorizontally(element, { smooth: false });

    expect(window.scrollTo).toBeCalledWith(445, 0);
  });

  it('should scroll window left to element', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ left: -10, right: 10 });

    Object.defineProperty(window, 'innerWidth', { value: 50 });
    window.scrollTo = jest.fn();

    document.body.appendChild(element);

    scrollHorizontally(element, { smooth: false });

    expect(window.scrollTo).toBeCalledWith(-10, 0);
  });

  it('should scroll window right to element smoothly', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ left: 840, right: 845 });

    Object.defineProperty(window, 'innerWidth', { value: 400 });
    window.scrollTo = jest.fn();

    document.body.appendChild(element);

    scrollHorizontally(element, {});

    jest.runAllTimers();

    expect(window.scrollTo).toBeCalledTimes(10);
  });
});

it('correctly queues and processes multiple scroll calls', async () => {
  const element1 = document.createElement('a');
  const element2 = document.createElement('a');
  document.body.appendChild(element1);
  document.body.appendChild(element2);
  element1.getBoundingClientRect = mockGetBoundingClientRect({ left: 840, right: 845 });
  element2.getBoundingClientRect = mockGetBoundingClientRect({ top: -10, bottom: 10 });

  window.scrollTo = jest.fn();

  scrollHorizontally(element1, {});
  scrollToElement(element2, { smooth: false });

  jest.runAllTimers();
  await Promise.resolve(setImmediate);
  await Promise.resolve(setImmediate);

  expect(window.scrollTo).toBeCalledTimes(11);

  scrollHorizontally(element1, {});
  jest.runAllTimers();
  expect(window.scrollTo).toBeCalledTimes(21);
});

const mockGetBoundingClientRect = (overrides: Partial<ClientRect>) => () =>
  ({
    bottom: 0,
    height: 0,
    left: 0,
    right: 0,
    top: 0,
    width: 0,
    ...overrides,
  } as DOMRect);
