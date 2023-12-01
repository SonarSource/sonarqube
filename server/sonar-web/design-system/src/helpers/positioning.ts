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
/**
 * Positioning rules:
 * - Bottom = below the block, horizontally centered
 * - BottomLeft = below the block, horizontally left-aligned
 * - BottomRight = below the block, horizontally right-aligned
 * - Left = Left of the block, vertically centered
 * - LeftTop = on the left-side of the block, vertically top-aligned
 * - LeftBottom = on the left-side of the block, vertically bottom-aligned
 * - Right = Right of the block, vertically centered
 * - RightTop = on the right-side of the block, vertically top-aligned
 * - RightBottom = on the right-side of the block, vetically bottom-aligned
 * - Top = above the block, horizontally centered
 * - TopLeft = above the block, horizontally left-aligned
 * - TopRight = above the block, horizontally right-aligned
 */

export enum PopupPlacement {
  Bottom = 'bottom',
  BottomLeft = 'bottom-left',
  BottomRight = 'bottom-right',
  Left = 'left',
  LeftTop = 'left-top',
  LeftBottom = 'left-bottom',
  Right = 'right',
  RightTop = 'right-top',
  RightBottom = 'right-bottom',
  Top = 'top',
  TopLeft = 'top-left',
  TopRight = 'top-right',
}

export enum PopupZLevel {
  Content = 'content',
  Default = 'popup',
  Global = 'global',
  Absolute = 'absolute',
}

export type BasePlacement = Extract<
  PopupPlacement,
  PopupPlacement.Bottom | PopupPlacement.Top | PopupPlacement.Left | PopupPlacement.Right
>;

export const PLACEMENT_FLIP_MAP: { [key in PopupPlacement]: PopupPlacement } = {
  [PopupPlacement.Left]: PopupPlacement.Right,
  [PopupPlacement.LeftBottom]: PopupPlacement.RightBottom,
  [PopupPlacement.LeftTop]: PopupPlacement.RightTop,
  [PopupPlacement.Right]: PopupPlacement.Left,
  [PopupPlacement.RightBottom]: PopupPlacement.LeftBottom,
  [PopupPlacement.RightTop]: PopupPlacement.LeftTop,
  [PopupPlacement.Top]: PopupPlacement.Bottom,
  [PopupPlacement.TopLeft]: PopupPlacement.BottomLeft,
  [PopupPlacement.TopRight]: PopupPlacement.BottomRight,
  [PopupPlacement.Bottom]: PopupPlacement.Top,
  [PopupPlacement.BottomLeft]: PopupPlacement.TopLeft,
  [PopupPlacement.BottomRight]: PopupPlacement.TopRight,
};

const MARGIN_TO_EDGE = 4;

export function popupPositioning(
  toggleNode: Element,
  popupNode: Element,
  placement: PopupPlacement = PopupPlacement.Bottom,
) {
  const toggleRect = toggleNode.getBoundingClientRect();
  const popupRect = popupNode.getBoundingClientRect();

  let { left, top } = toggleRect;

  switch (placement) {
    case PopupPlacement.Bottom:
      left += toggleRect.width / 2 - popupRect.width / 2;
      top += toggleRect.height;
      break;
    case PopupPlacement.BottomLeft:
      top += toggleRect.height;
      break;
    case PopupPlacement.BottomRight:
      left += toggleRect.width - popupRect.width;
      top += toggleRect.height;
      break;
    case PopupPlacement.Left:
      left -= popupRect.width;
      top += toggleRect.height / 2 - popupRect.height / 2;
      break;
    case PopupPlacement.LeftTop:
      left -= popupRect.width;
      break;
    case PopupPlacement.LeftBottom:
      left -= popupRect.width;
      top += toggleRect.height - popupRect.height;
      break;
    case PopupPlacement.Right:
      left += toggleRect.width;
      top += toggleRect.height / 2 - popupRect.height / 2;
      break;
    case PopupPlacement.RightTop:
      left += toggleRect.width;
      break;
    case PopupPlacement.RightBottom:
      left += toggleRect.width;
      top += toggleRect.height - popupRect.height;
      break;
    case PopupPlacement.Top:
      left += toggleRect.width / 2 - popupRect.width / 2;
      top -= popupRect.height;
      break;
    case PopupPlacement.TopLeft:
      top -= popupRect.height;
      break;
    case PopupPlacement.TopRight:
      left += toggleRect.width - popupRect.width;
      top -= popupRect.height;
      break;
  }

  const inBoundariesLeft = Math.min(
    Math.max(left, getMinLeftPlacement(toggleRect)),
    getMaxLeftPlacement(toggleRect, popupRect),
  );

  const inBoundariesTop = Math.min(
    Math.max(top, getMinTopPlacement(toggleRect)),
    getMaxTopPlacement(toggleRect, popupRect),
  );

  return {
    height: popupRect.height,
    left: inBoundariesLeft,
    leftFix: inBoundariesLeft - left,
    top: inBoundariesTop,
    topFix: inBoundariesTop - top,
    width: popupRect.width,
  };
}

function getMinLeftPlacement(toggleRect: DOMRect) {
  return Math.min(
    MARGIN_TO_EDGE, // Left edge of the sceen
    toggleRect.left + toggleRect.width / 2, // Left edge of the screen when scrolled
  );
}

function getMaxLeftPlacement(toggleRect: DOMRect, popupRect: DOMRect) {
  return Math.max(
    document.documentElement.clientWidth - popupRect.width - MARGIN_TO_EDGE, // Right edge of the screen
    toggleRect.left + toggleRect.width / 2 - popupRect.width, // Right edge of the screen when scrolled
  );
}

function getMinTopPlacement(toggleRect: DOMRect) {
  return Math.min(
    MARGIN_TO_EDGE, // Top edge of the sceen
    toggleRect.top + toggleRect.height / 2, // Top edge of the screen when scrolled
  );
}

function getMaxTopPlacement(toggleRect: DOMRect, popupRect: DOMRect) {
  return Math.max(
    document.documentElement.clientHeight - popupRect.height - MARGIN_TO_EDGE, // Bottom edge of the screen
    toggleRect.top + toggleRect.height / 2 - popupRect.height, // Bottom edge of the screen when scrolled
  );
}
