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
import { throttle } from 'lodash';
import { useCallback, useEffect, useRef, useState } from 'react';

const THROTTLE_LONG_DELAY = 100;

export default function useScrollDownCompress(compressThreshold: number, scrollThreshold: number) {
  const [isCompressed, setIsCompressed] = useState(false);
  const [isScrolled, setIsScrolled] = useState(
    () => document?.documentElement?.scrollTop > scrollThreshold
  );

  const initialScrollHeightRef = useRef<number | undefined>(undefined);
  const scrollTopRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    const handleScroll = throttle(() => {
      // Save the initial scrollHeight of the document
      const scrollHeight = document?.documentElement?.scrollHeight;
      initialScrollHeightRef.current = Math.max(initialScrollHeightRef.current ?? 0, scrollHeight);

      // Compute the scrollTop value relative to the initial scrollHeight.
      // The scrollHeight value changes when we compress the header - influencing the scrollTop value
      const relativeScrollTop =
        document?.documentElement?.scrollTop + (initialScrollHeightRef.current - scrollHeight);

      if (
        // First scroll means we just loaded the page or changed tab, in this case we shouldn't compress
        scrollTopRef.current === undefined ||
        // We also shouldn't compress if the size of the document wouldn't have a scroll after being compressed
        initialScrollHeightRef.current - document?.documentElement?.clientHeight < compressThreshold
      ) {
        setIsCompressed(false);

        // We shouldn't change the compressed flag if the scrollTop value didn't change
      } else if (relativeScrollTop !== scrollTopRef.current) {
        // Compress when scrolling in down direction and we are scrolled more than a threshold
        setIsCompressed(
          relativeScrollTop > scrollTopRef.current && relativeScrollTop > scrollThreshold
        );
      }

      // Should display the shadow when we are scrolled more than a small threshold
      setIsScrolled(relativeScrollTop > scrollThreshold);

      // Save the last scroll position to compare it with the next one and infer the directions
      scrollTopRef.current = relativeScrollTop;
    }, THROTTLE_LONG_DELAY);

    document.addEventListener('scroll', handleScroll);
    return () => document.removeEventListener('scroll', handleScroll);
  }, []);

  const resetScrollDownCompress = useCallback(() => {
    initialScrollHeightRef.current = undefined;
    scrollTopRef.current = undefined;
  }, []);

  return { isCompressed, isScrolled, resetScrollDownCompress };
}
