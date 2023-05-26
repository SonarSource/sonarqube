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
import styled from '@emotion/styled';
import { scaleLinear } from 'd3-scale';
import React from 'react';
import tw from 'twin.macro';
import { themeColor } from '../helpers';
import { Key } from '../helpers/keyboard';
import { BasePlacement, PopupPlacement } from '../helpers/positioning';
import Tooltip from './Tooltip';

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
  function handleRectClick() {
    props.onClick(props.itemKey);
  }

  function handleRectKeyDown(event: React.KeyboardEvent<HTMLAnchorElement>) {
    if (event.key === Key.Enter) {
      props.onClick(props.itemKey);
    }
  }

  function renderCell() {
    const cellStyles = {
      left: props.x,
      top: props.y,
      width: props.width,
      height: props.height,
      backgroundColor: props.fill,
      backgroundImage: props.gradient,
      fontSize: SIZE_SCALE(props.width / props.label.length),
      lineHeight: `${props.height}px`,
    };
    const isTextVisible =
      props.width >= TEXT_VISIBLE_AT_WIDTH && props.height >= TEXT_VISIBLE_AT_HEIGHT;
    const isIconVisible =
      props.width >= ICON_VISIBLE_AT_WIDTH && props.height >= ICON_VISIBLE_AT_HEIGHT;

    return (
      <StyledCell style={cellStyles}>
        <StyledCellLink
          aria-label={props.prefix ? `${props.prefix} ${props.label}` : props.label}
          onClick={handleRectClick}
          onKeyDown={handleRectKeyDown}
          role="link"
          tabIndex={0}
        >
          <StyledCellLabel width={props.width}>
            {isIconVisible && <span className="shrink-0">{props.icon}</span>}
            {isTextVisible &&
              (props.prefix ? (
                <span className="treemap-text">
                  {props.prefix}
                  <br />
                  {props.label.substring(props.prefix.length)}
                </span>
              ) : (
                <span className="treemap-text">{props.label}</span>
              ))}
          </StyledCellLabel>
        </StyledCellLink>
      </StyledCell>
    );
  }

  const { placement, tooltip } = props;
  return (
    <Tooltip overlay={tooltip} placement={placement ?? PopupPlacement.Left}>
      {renderCell()}
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
