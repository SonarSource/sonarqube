/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { scrollToElement } from '../scrolling';

beforeAll(() => {
  jest.useFakeTimers();
});

afterAll(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

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

    expect(window.scrollTo).toHaveBeenCalledWith(0, 445);
  });

  it('should scroll window up to element', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ top: -10, bottom: 10 });

    Object.defineProperty(window, 'innerHeight', { value: 50 });
    window.scrollTo = jest.fn();

    document.body.appendChild(element);

    scrollToElement(element, { smooth: false });

    expect(window.scrollTo).toHaveBeenCalledWith(0, -10);
  });

  it('should scroll window down to element smoothly', () => {
    const element = document.createElement('a');
    element.getBoundingClientRect = mockGetBoundingClientRect({ top: 840, bottom: 845 });

    Object.defineProperty(window, 'innerHeight', { value: 400 });
    window.scrollTo = jest.fn();

    document.body.appendChild(element);

    scrollToElement(element, {});

    jest.runAllTimers();

    expect(window.scrollTo).toHaveBeenCalledTimes(10);
  });
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
