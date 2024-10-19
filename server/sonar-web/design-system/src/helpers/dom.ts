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
/**
 * This function find the point on the target element using the rect coordinat.
 * This point will serve as an anchor for rect to be attached
 *
 * This function assume that rect side will overlap the target facing side. If not
 * the case the point would be outside the target rect. For now we don't need to
 * handle this situation.
 * @param rect
 * @param targetRect
 */
export function findAnchor(rect: DOMRect, targetRect: DOMRect, offset: number) {
  const offestTop = rect.top < targetRect.top ? targetRect.top - rect.top : 0;
  const offestLeft = rect.left < targetRect.left ? targetRect.left - rect.left : 0;

  if (targetRect.right < rect.left) {
    const left = targetRect.right - rect.left - offset / 2;
    const top =
      (Math.min(targetRect.bottom, rect.bottom) - Math.max(targetRect.top, rect.top)) / 2 -
      offset / 2 +
      offestTop;
    const rotate = '0deg';
    const width = -left - offset;

    return { left, top, rotate, width };
  } else if (targetRect.left > rect.right) {
    const left = rect.width + targetRect.left - rect.right + offset / 2;
    const top =
      (Math.min(targetRect.bottom, rect.bottom) - Math.max(targetRect.top, rect.top)) / 2 -
      offset / 2 +
      offestTop;
    const rotate = '180deg';
    const width = left - rect.width;
    return { left, top, rotate, width };
  } else if (targetRect.bottom < rect.top) {
    const left =
      (Math.min(targetRect.right, rect.right) - Math.max(targetRect.left, rect.left)) / 2 -
      offset / 2 +
      offestLeft;
    const top = targetRect.bottom - rect.top - offset / 2;
    const rotate = '90deg';
    const width = -top - offset;
    return { left, top, rotate, width };
  } else if (targetRect.top > rect.bottom) {
    const left =
      (Math.min(targetRect.right, rect.right) - Math.max(targetRect.left, rect.left)) / 2 -
      offset / 2 +
      offestLeft;
    const top = targetRect.top - rect.top - offset / 2;
    const rotate = '-90deg';
    const width = top - rect.height;
    return { left, top, rotate, width };
  }

  // When rectagle overlap
  return { left: 0, top: 0, rotate: '0deg', width: 0 };
}
