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

import { useEffect, useState } from 'react';

interface Options {
  direction?: 'HORIZONTAL' | 'VERTICAL';
  offset: number;
}

/*
 * Detects if sticky element is out of viewport
 */
export default function useStickyDetection(target: string, options: Options) {
  const { offset, direction = 'VERTICAL' } = options;
  const [isSticky, setIsSticky] = useState(false);

  useEffect(() => {
    const rootMargin =
      direction === 'VERTICAL' ? `${-offset - 1}px 0px 0px 0px` : `0px 0px 0px ${-offset - 1}px`;

    const observer = new IntersectionObserver(
      ([e]) => {
        setIsSticky(e.intersectionRatio < 1 && elementIntersectedByDirection(e, direction));
      },
      // -1px moves viewport by direction that allows to detect when element became sticky and
      // fully visible in viewport
      { threshold: [1], rootMargin },
    );

    const element = document.querySelector(target);

    if (element) {
      observer.observe(element);
    }

    return () => {
      if (element) {
        observer.unobserve(element);
      }
    };
  }, [target, setIsSticky, direction, offset]);

  return isSticky;
}

function elementIntersectedByDirection(
  e: IntersectionObserverEntry,
  direction: 'VERTICAL' | 'HORIZONTAL',
) {
  const { boundingClientRect, intersectionRect } = e;
  const prop = direction === 'VERTICAL' ? 'top' : 'right';

  return boundingClientRect[prop] - intersectionRect[prop] !== 0;
}
