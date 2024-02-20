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
/* eslint-disable prefer-destructuring */

import classNames from 'classnames';
import * as React from 'react';
import ClickEventBoundary from '../controls/ClickEventBoundary';
import './popups.css';

/**
 * Positioning rules:
 * - Bottom = below the block, horizontally centered
 * - BottomLeft = below the block, horizontally left-aligned
 * - BottomRight = below the block, horizontally right-aligned
 * - LeftTop = on the left-side of the block, vertically top-aligned
 * - RightTop = on the right-side of the block, vertically top-aligned
 * - RightBottom = on the right-side of the block, vetically bottom-aligned
 * - TopLeft = above the block, horizontally left-aligned
 */
export enum PopupPlacement {
  Bottom = 'bottom',
  BottomLeft = 'bottom-left',
  BottomRight = 'bottom-right',
  LeftTop = 'left-top',
  RightTop = 'right-top',
  RightBottom = 'right-bottom',
  TopLeft = 'top-left',
}

interface PopupProps {
  arrowStyle?: React.CSSProperties;
  children?: React.ReactNode;
  className?: string;
  noArrow?: boolean;
  noPadding?: boolean;
  placement?: PopupPlacement;
  style?: React.CSSProperties;
  useEventBoundary?: boolean;
}

function PopupBase(props: PopupProps, ref: React.Ref<HTMLDivElement>) {
  const { useEventBoundary = true, noArrow = false, placement = PopupPlacement.Bottom } = props;
  const inner = (
    <div
      className={classNames(
        'popup',
        `is-${placement}`,
        { 'no-padding': props.noPadding },
        props.className,
      )}
      ref={ref || React.createRef()}
      style={props.style}
    >
      {props.children}
      {!noArrow && <PopupArrow style={props.arrowStyle} />}
    </div>
  );
  if (useEventBoundary) {
    return <ClickEventBoundary>{inner}</ClickEventBoundary>;
  }
  return inner;
}

interface PopupArrowProps {
  style?: React.CSSProperties;
}

function PopupArrow(props: PopupArrowProps) {
  return <div className="popup-arrow" style={props.style} />;
}

const PopupWithRef = React.forwardRef(PopupBase);
PopupWithRef.displayName = 'Popup';
export const Popup = PopupWithRef;
