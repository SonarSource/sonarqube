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
import { debounce } from 'lodash';

const SCROLLING_DURATION = 100;
const SCROLLING_INTERVAL = 10;
const SCROLLING_STEPS = SCROLLING_DURATION / SCROLLING_INTERVAL;

function isWindow(element: Element | Window): element is Window {
  return element === window;
}

function getScroll(element: Element | Window) {
  return isWindow(element)
    ? { x: window.pageXOffset, y: window.pageYOffset }
    : { x: element.scrollLeft, y: element.scrollTop };
}

function scrollElement(element: Element | Window, x: number, y: number): void {
  if (isWindow(element)) {
    window.scrollTo(x, y);
  } else {
    element.scrollLeft = x;
    element.scrollTop = y;
  }
}

let smoothScroll = (target: number, current: number, scroll: (position: number) => void) => {
  const positiveDirection = target > current;
  const step = Math.ceil(Math.abs(target - current) / SCROLLING_STEPS);
  let stepsDone = 0;

  const interval = setInterval(() => {
    if (current === target || SCROLLING_STEPS === stepsDone) {
      clearInterval(interval);
    } else {
      let goal;
      if (positiveDirection) {
        goal = Math.min(target, current + step);
      } else {
        goal = Math.max(target, current - step);
      }
      stepsDone++;
      current = goal;
      scroll(goal);
    }
  }, SCROLLING_INTERVAL);
};
smoothScroll = debounce(smoothScroll, SCROLLING_DURATION, { leading: true });

function smoothScrollTop(position: number, parent: Element | Window) {
  const scroll = getScroll(parent);
  smoothScroll(position, scroll.y, position => scrollElement(parent, scroll.x, position));
}

function smoothScrollLeft(position: number, parent: Element | Window) {
  const scroll = getScroll(parent);
  smoothScroll(position, scroll.x, position => scrollElement(parent, position, scroll.y));
}

export function scrollToElement(
  element: Element,
  options: {
    topOffset?: number;
    bottomOffset?: number;
    parent?: Element;
    smooth?: boolean;
  }
): void {
  const opts = { topOffset: 0, bottomOffset: 0, parent: window, smooth: true, ...options };
  const { parent } = opts;

  const { top, bottom } = element.getBoundingClientRect();

  const scroll = getScroll(parent);

  const height: number = isWindow(parent)
    ? window.innerHeight
    : parent.getBoundingClientRect().height;

  const parentTop = isWindow(parent) ? 0 : parent.getBoundingClientRect().top;

  if (top - parentTop < opts.topOffset) {
    const goal = scroll.y - opts.topOffset + top - parentTop;
    if (opts.smooth) {
      smoothScrollTop(goal, parent);
    } else {
      scrollElement(parent, scroll.x, goal);
    }
  }

  if (bottom - parentTop > height - opts.bottomOffset) {
    const goal = scroll.y + bottom - parentTop - height + opts.bottomOffset;
    if (opts.smooth) {
      smoothScrollTop(goal, parent);
    } else {
      scrollElement(parent, scroll.x, goal);
    }
  }
}

export function scrollHorizontally(
  element: Element,
  options: {
    leftOffset?: number;
    rightOffset?: number;
    parent?: Element;
    smooth?: boolean;
  }
): void {
  const opts = { leftOffset: 0, rightOffset: 0, parent: window, smooth: true, ...options };
  const { parent } = opts;

  const { left, right } = element.getBoundingClientRect();

  const scroll = getScroll(parent);

  const { left: parentLeft, width } = isWindow(parent)
    ? { left: 0, width: window.innerWidth }
    : parent.getBoundingClientRect();

  if (left - parentLeft < opts.leftOffset) {
    const goal = scroll.x - opts.leftOffset + left - parentLeft;
    if (opts.smooth) {
      smoothScrollLeft(goal, parent);
    } else {
      scrollElement(parent, goal, scroll.y);
    }
  }

  if (right - parentLeft > width - opts.rightOffset) {
    const goal = scroll.x + right - parentLeft - width + opts.rightOffset;
    if (opts.smooth) {
      smoothScrollLeft(goal, parent);
    } else {
      scrollElement(parent, goal, scroll.y);
    }
  }
}
