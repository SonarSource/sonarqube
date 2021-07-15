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

function scrollElement(element: Element | Window, x: number, y: number): Promise<void> {
  if (isWindow(element)) {
    window.scrollTo(x, y);
  } else {
    element.scrollLeft = x;
    element.scrollTop = y;
  }
  return Promise.resolve();
}

function smoothScroll(
  target: number,
  current: number,
  scroll: (position: number) => void
): Promise<void> {
  const positiveDirection = target > current;
  const step = Math.ceil(Math.abs(target - current) / SCROLLING_STEPS);
  let stepsDone = 0;

  return new Promise((resolve) => {
    const interval = setInterval(() => {
      if (current === target || SCROLLING_STEPS === stepsDone) {
        clearInterval(interval);
        resolve();
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
  });
}

function smoothScrollTop(parent: Element | Window, position: number) {
  const scroll = getScroll(parent);
  return smoothScroll(position, scroll.y, (position) => scrollElement(parent, scroll.x, position));
}

function smoothScrollLeft(parent: Element | Window, position: number) {
  const scroll = getScroll(parent);
  return smoothScroll(position, scroll.x, (position) => scrollElement(parent, position, scroll.y));
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
      addToScrollQueue(smoothScrollTop, parent, goal);
    } else {
      addToScrollQueue(scrollElement, parent, scroll.x, goal);
    }
  }

  if (bottom - parentTop > height - opts.bottomOffset) {
    const goal = scroll.y + bottom - parentTop - height + opts.bottomOffset;
    if (opts.smooth) {
      addToScrollQueue(smoothScrollTop, parent, goal);
    } else {
      addToScrollQueue(scrollElement, parent, scroll.x, goal);
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
      addToScrollQueue(smoothScrollLeft, parent, goal);
    } else {
      addToScrollQueue(scrollElement, parent, goal, scroll.y);
    }
  }

  if (right - parentLeft > width - opts.rightOffset) {
    const goal = scroll.x + right - parentLeft - width + opts.rightOffset;
    if (opts.smooth) {
      addToScrollQueue(smoothScrollLeft, parent, goal);
    } else {
      addToScrollQueue(scrollElement, parent, goal, scroll.y);
    }
  }
}

type ScrollFunction = (element: Element | Window, x: number, y?: number) => Promise<void>;

interface ScrollQueueItem {
  element: Element | Window;
  fn: ScrollFunction;
  x: number;
  y?: number;
}

const queue: ScrollQueueItem[] = [];
let queueRunning: boolean;

function addToScrollQueue(
  fn: ScrollFunction,
  element: Element | Window,
  x: number,
  y?: number
): void {
  queue.push({ fn, element, x, y });
  if (!queueRunning) {
    processQueue();
  }
}

function processQueue() {
  if (queue.length > 0) {
    queueRunning = true;
    const { fn, element, x, y } = queue.shift()!;
    fn(element, x, y).then(processQueue).catch(processQueue);
  } else {
    queueRunning = false;
  }
}
