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
import { scaleLinear } from 'd3-scale';
import React from 'react';
import tw from 'twin.macro';
import { themeColor } from '../helpers';
import { BasePlacement, PopupPlacement } from '../helpers/positioning';
import { Tooltip } from './Tooltip';

const SIZE_SCALE = scaleLinear().domain([3, 15]).range([11, 18]).clamp(true);
const TEXT_VISIBLE_AT_WIDTH = 40;
const TEXT_VISIBLE_AT_HEIGHT = 45;
const ICON_VISIBLE_AT_WIDTH = 24;
const ICON_VISIBLE_AT_HEIGHT = 26;

interface Props {
  fill?: string;
  gradient?: string;
  height: number;
  icon?: React.ReactNode;
  itemKey: string;
  label: string;
  onClick: (item: string) => void;
  placement?: BasePlacement;
  prefix: string;
  tooltip?: React.ReactNode;
  width: number;
  x: number;
  y: number;
}

export function TreeMapRect(props: Props) {
  const {
    placement,
    tooltip,
    onClick,
    itemKey,
    x,
    y,
    width,
    height,
    fill,
    gradient,
    label,
    icon,
    prefix,
  } = props;

  const handleRectClick = React.useCallback(() => {
    onClick(itemKey);
    return false;
  }, [onClick, itemKey]);

  const cellStyles = {
    left: x,
    top: y,
    width,
    height,
    backgroundColor: fill,
    backgroundImage: gradient,
    fontSize: SIZE_SCALE(width / label.length),
    lineHeight: `${height}px`,
  };
  const isTextVisible = width >= TEXT_VISIBLE_AT_WIDTH && height >= TEXT_VISIBLE_AT_HEIGHT;
  const isIconVisible = width >= ICON_VISIBLE_AT_WIDTH && height >= ICON_VISIBLE_AT_HEIGHT;

  return (
    <Tooltip overlay={tooltip} placement={placement ?? PopupPlacement.Left}>
      <StyledCell style={cellStyles}>
        <StyledCellLink href="#" onClick={handleRectClick}>
          <StyledCellLabel width={width}>
            {isIconVisible && <span className="shrink-0">{icon}</span>}
            {isTextVisible &&
              (prefix ? (
                <span className="treemap-text">
                  {prefix}
                  <br />
                  {label.substring(prefix.length)}
                </span>
              ) : (
                <span className="treemap-text">{label}</span>
              ))}
            <StyledA11yHidden>{tooltip}</StyledA11yHidden>
          </StyledCellLabel>
        </StyledCellLink>
      </StyledCell>
    </Tooltip>
  );
}

const StyledCell = styled.li`
  ${tw`sw-absolute`};
  ${tw`sw-box-border`};

  border-right: 1px solid #fff;
  border-bottom: 1px solid #fff;
`;

const StyledCellLink = styled.a`
  ${tw`sw-w-full sw-h-full`};
  ${tw`sw-border-0`};
  ${tw`sw-flex sw-flex-col sw-items-center sw-justify-center`};

  color: ${themeColor('pageContent')};

  &:hover,
  &:active,
  &:focus {
    ${tw`sw-border-0`};
    outline: none;
  }

  &:focus .treemap-text,
  &:hover .treemap-text {
    ${tw`sw-underline`};
  }
`;

const StyledCellLabel = styled.div<{ width: number }>`
  ${tw`sw-flex sw-flex-wrap sw-justify-center sw-items-center sw-gap-2`};

  line-height: 1.2;
  max-width: ${({ width }) => width}px;

  .treemap-text {
    ${tw`sw-shrink sw-overflow-hidden sw-whitespace-nowrap sw-text-left sw-text-ellipsis`};
  }
`;

const StyledA11yHidden = styled.span`
  position: absolute !important;
  left: -10000px !important;
  top: auto !important;
  width: 1px !important;
  height: 1px !important;
  overflow: hidden !important;
`;
