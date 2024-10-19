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
import { CSSColor } from '../types/theme';

/* eslint-disable no-bitwise, no-mixed-operators */

export function stringToColor(str: string) {
  let hash = 0;

  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }

  let color = '#';

  for (let i = 0; i < 3; i++) {
    const value = (hash >> (i * 8)) & 0xff;
    color += value.toString(16).padStart(2, '0');
  }

  return color;
}

export function isDarkColor(color: string) {
  color = color.substring(1);

  if (color.length === 3) {
    // shortcut notation: #f90
    color = color[0] + color[0] + color[1] + color[1] + color[2] + color[2];
  }

  const rgb = parseInt(color.substring(1), 16);
  const r = (rgb >> 16) & 0xff;
  const g = (rgb >> 8) & 0xff;
  const b = (rgb >> 0) & 0xff;
  const luma = 0.2126 * r + 0.7152 * g + 0.0722 * b;

  return luma < 140;
}

export function getTextColor(background: string, dark = '#222', light = '#fff') {
  return isDarkColor(background) ? light : dark;
}

export function getRGBAString([r, g, b]: Array<number | string>, a?: number | string) {
  return (a !== undefined ? `rgba(${r},${g},${b},${a})` : `rgb(${r},${g},${b})`) as CSSColor;
}
