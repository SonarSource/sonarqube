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
import styled from '@emotion/styled';
import { Badge, HotspotRating, HotspotRatingEnum, SubnavigationAccordion } from 'design-system';
import React, { memo } from 'react';
import { RawHotspot } from '../../../types/security-hotspots';
import HotspotListItem from './HotspotListItem';

interface HotspotCategoryProps {
  expanded: boolean;
  onSetExpanded: (expanded: boolean) => void;
  hotspots: RawHotspot[];
  isLastAndIncomplete: boolean;
  onHotspotClick: (hotspot: RawHotspot) => void;
  onLocationClick: (index: number) => void;
  rating: HotspotRatingEnum;
  selectedHotspot: RawHotspot;
  selectedHotspotLocation?: number;
  title: string;
}

export default function HotspotCategory(props: HotspotCategoryProps) {
  const {
    expanded,
    onSetExpanded,
    hotspots,
    onLocationClick,
    onHotspotClick,
    rating,
    selectedHotspot,
    title,
    isLastAndIncomplete,
    selectedHotspotLocation,
  } = props;

  if (hotspots.length < 1) {
    return null;
  }

  const risk = hotspots[0].vulnerabilityProbability;

  return (
    <SubnavigationAccordion
      header={
        <MemoizedHeader
          hotspots={hotspots}
          isLastAndIncomplete={isLastAndIncomplete}
          rating={rating}
          title={title}
        />
      }
      id={`hotspot-category-${risk}`}
      expanded={expanded}
      onSetExpanded={onSetExpanded}
    >
      <ul>
        {hotspots.map((hotspot) => (
          <li key={hotspot.key}>
            <HotspotListItem
              hotspot={hotspot}
              onClick={onHotspotClick}
              selected={hotspot.key === selectedHotspot.key}
              onLocationClick={onLocationClick}
              selectedHotspotLocation={selectedHotspotLocation}
            />
          </li>
        ))}
      </ul>
    </SubnavigationAccordion>
  );
}

type NavigationHeaderProps = Pick<
  HotspotCategoryProps,
  'hotspots' | 'isLastAndIncomplete' | 'rating' | 'title'
>;

function NavigationHeader(props: NavigationHeaderProps) {
  const { hotspots, isLastAndIncomplete, rating, title } = props;
  const counter = hotspots.length + (isLastAndIncomplete ? '+' : '');

  return (
    <SubNavigationContainer className="sw-flex sw-justify-between">
      <div className="sw-flex sw-items-center">
        <HotspotRating className="sw-mr-2" rating={rating} />
        {title}
      </div>
      <Badge variant="counter">{counter}</Badge>
    </SubNavigationContainer>
  );
}

const MemoizedHeader = memo(NavigationHeader);

const SubNavigationContainer = styled.div`
  width: calc(100% - 1.5rem);
`;
