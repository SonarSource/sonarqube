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

import React from 'react';

(window as any).React = React;

const baseUrl = '';
(window as any).baseUrl = baseUrl;

jest.mock('../../src/main/js/helpers/l10n', () => ({
  ...jest.requireActual('../../src/main/js/helpers/l10n'),
  hasMessage: () => true,
  translate: (...keys: string[]) => keys.join('.'),
  translateWithParameters: (messageKey: string, ...parameters: Array<string | number>) =>
    [messageKey, ...parameters].join('.'),
}));

global.___loader = {
  enqueue: jest.fn(),
};

const MockObserver = {
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn(),
};

const MockIntersectionObserverEntries = [{ isIntersecting: true }];

(window as any).IntersectionObserver = jest.fn().mockImplementation((callback) => {
  callback(MockIntersectionObserverEntries, MockObserver);
  return MockObserver;
});

Element.prototype.scrollIntoView = () => {};

const content = document.createElement('div');
content.id = 'content';
document.documentElement.appendChild(content);

// ResizeObserver

const MockResizeObserverEntries = [
  {
    contentRect: {
      width: 100,
      height: 200,
    },
  },
];

const MockResizeObserver = {
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn(),
};

global.ResizeObserver = jest.fn().mockImplementation((callback) => {
  callback(MockResizeObserverEntries, MockResizeObserver);
  return MockResizeObserver;
});

// Copied from pollyfill.io
// To be remove when upgrading jsdom https://github.com/jsdom/jsdom/releases/tag/22.1.0
// jest-environment-jsdom to v30
function number(v) {
  return v === undefined ? 0 : Number(v);
}

function different(u, v) {
  return u !== v && !(isNaN(u) && isNaN(v));
}

global.DOMRect = function DOMRect(xArg, yArg, wArg, hArg) {
  let x;
  let y;
  let width;
  let height;
  let left;
  let right;
  let top;
  let bottom;

  x = number(xArg);
  y = number(yArg);
  width = number(wArg);
  height = number(hArg);

  Object.defineProperties(this, {
    x: {
      get() {
        return x;
      },
      set(newX) {
        if (different(x, newX)) {
          x = newX;
          left = undefined;
          right = undefined;
        }
      },
      enumerable: true,
    },
    y: {
      get() {
        return y;
      },
      set(newY) {
        if (different(y, newY)) {
          y = newY;
          top = undefined;
          bottom = undefined;
        }
      },
      enumerable: true,
    },
    width: {
      get() {
        return width;
      },
      set(newWidth) {
        if (different(width, newWidth)) {
          width = newWidth;
          left = undefined;
          right = undefined;
        }
      },
      enumerable: true,
    },
    height: {
      get() {
        return height;
      },
      set(newHeight) {
        if (different(height, newHeight)) {
          height = newHeight;
          top = undefined;
          bottom = undefined;
        }
      },
      enumerable: true,
    },
    left: {
      get() {
        if (left === undefined) {
          left = x + Math.min(0, width);
        }
        return left;
      },
      enumerable: true,
    },
    right: {
      get() {
        if (right === undefined) {
          right = x + Math.max(0, width);
        }
        return right;
      },
      enumerable: true,
    },
    top: {
      get() {
        if (top === undefined) {
          top = y + Math.min(0, height);
        }
        return top;
      },
      enumerable: true,
    },
    bottom: {
      get() {
        if (bottom === undefined) {
          bottom = y + Math.max(0, height);
        }
        return bottom;
      },
      enumerable: true,
    },
  });
};
