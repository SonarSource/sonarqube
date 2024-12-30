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

import { RefObject, useEffect, useState } from 'react';
import { isDefined } from '../helpers/types';

interface Options extends IntersectionObserverInit {
  freezeOnceVisible?: boolean;
}

export default function useIntersectionObserver<T extends Element>(
  ref: RefObject<T>,
  options: Options = {},
) {
  const { root = null, rootMargin = '0px', threshold = 0, freezeOnceVisible = false } = options;
  const [entry, setEntry] = useState<IntersectionObserverEntry>();

  const frozen = (entry?.isIntersecting || false) && freezeOnceVisible;

  useEffect(() => {
    if (!isDefined(IntersectionObserver) || !isDefined(ref.current) || frozen) {
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => setEntry(entry),

      { root, rootMargin, threshold },
    );

    observer.observe(ref.current);
    return () => observer.disconnect();
  }, [ref, frozen, root, rootMargin, threshold]);

  return entry;
}
