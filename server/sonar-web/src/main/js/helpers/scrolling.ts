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

function getScrollPosition(element: Element | Window): number {
  return isWindow(element) ? window.pageYOffset : element.scrollTop;
}

function scrollElement(element: Element | Window, position: number): void {
  if (isWindow(element)) {
    window.scrollTo(0, position);
  } else {
    element.scrollTop = position;
  }
}

let smoothScrollTop = (y: number, parent: Element | Window) => {
  let scrollTop = getScrollPosition(parent);
  const scrollingDown = y > scrollTop;
  const step = Math.ceil(Math.abs(y - scrollTop) / SCROLLING_STEPS);
  let stepsDone = 0;

  const interval = setInterval(() => {
    if (scrollTop === y || SCROLLING_STEPS === stepsDone) {
      clearInterval(interval);
    } else {
      let goal;
      if (scrollingDown) {
        goal = Math.min(y, scrollTop + step);
      } else {
        goal = Math.max(y, scrollTop - step);
      }
      stepsDone++;
      scrollTop = goal;
      scrollElement(parent, goal);
    }
  }, SCROLLING_INTERVAL);
};

smoothScrollTop = debounce(smoothScrollTop, SCROLLING_DURATION, { leading: true });

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

  const scrollTop = getScrollPosition(parent);

  const height: number = isWindow(parent)
    ? window.innerHeight
    : parent.getBoundingClientRect().height;

  const parentTop = isWindow(parent) ? 0 : parent.getBoundingClientRect().top;

  if (top - parentTop < opts.topOffset) {
    const goal = scrollTop - opts.topOffset + top - parentTop;
    if (opts.smooth) {
      smoothScrollTop(goal, parent);
    } else {
      scrollElement(parent, goal);
    }
  }

  if (bottom - parentTop > height - opts.bottomOffset) {
    const goal = scrollTop + bottom - parentTop - height + opts.bottomOffset;
    if (opts.smooth) {
      smoothScrollTop(goal, parent);
    } else {
      scrollElement(parent, goal);
    }
  }
}
