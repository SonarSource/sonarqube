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

import * as React from 'react';
import { IssueMessageHighlighting, LineFinding } from '~design-system';
import { Hotspot } from '../../../types/security-hotspots';

const SCROLL_DELAY = 100;
const SCROLL_TOP_OFFSET = 100; // 5 lines above
const SCROLL_BOTTOM_OFFSET = 28; // 1 line below + margin

export interface HotspotPrimaryLocationBoxProps {
  hotspot: Hotspot;
  secondaryLocationSelected: boolean;
}

export default function HotspotPrimaryLocationBox(props: HotspotPrimaryLocationBoxProps) {
  const { hotspot, secondaryLocationSelected } = props;

  const locationRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (locationRef.current && !secondaryLocationSelected) {
      // We need this delay to let the parent resize itself before scrolling
      setTimeout(() => {
        locationRef.current?.scrollIntoView({
          block: 'nearest',
          behavior: 'smooth',
        });
      }, SCROLL_DELAY);
    }
  }, [locationRef, secondaryLocationSelected]);

  return (
    <div
      style={{
        scrollMarginTop: `${SCROLL_TOP_OFFSET}px`,
        scrollMarginBottom: `${SCROLL_BOTTOM_OFFSET}px`,
      }}
      ref={locationRef}
    >
      <LineFinding
        issueKey={hotspot.key}
        message={
          <IssueMessageHighlighting
            message={hotspot.message}
            messageFormattings={hotspot.messageFormattings}
          />
        }
        selected
        className="sw-cursor-default"
      />
    </div>
  );
}
