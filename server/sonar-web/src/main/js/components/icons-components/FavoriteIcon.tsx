/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as classNames from 'classnames';
import { IconProps } from './types';
import * as theme from '../../app/theme';

export interface Props extends IconProps {
  favorite: boolean;
}

export default function FavoriteIcon({
  className,
  favorite,
  fill = theme.orange,
  size = 16
}: Props) {
  return (
    <svg
      className={classNames('icon-outline', { 'is-filled': favorite }, className)}
      style={{ color: fill }}
      width={size}
      height={size}
      viewBox="0 0 16 16"
      version="1.1"
      xmlnsXlink="http://www.w3.org/1999/xlink"
      xmlSpace="preserve">
      <g transform="matrix(0.988024,0,0,0.988024,0.0957953,0.717719)">
        <path d="M15.428,5.777C15.428,5.908 15.35,6.051 15.195,6.205L11.954,9.366L12.722,13.83C12.728,13.872 12.731,13.932 12.731,14.009C12.731,14.134 12.7,14.24 12.637,14.326C12.575,14.412 12.484,14.455 12.365,14.455C12.252,14.455 12.133,14.42 12.008,14.348L7.999,12.241L3.99,14.348C3.859,14.42 3.74,14.455 3.633,14.455C3.508,14.455 3.414,14.412 3.352,14.326C3.289,14.24 3.258,14.134 3.258,14.009C3.258,13.973 3.264,13.914 3.276,13.83L4.044,9.366L0.794,6.205C0.645,6.045 0.57,5.902 0.57,5.777C0.57,5.557 0.737,5.42 1.07,5.366L5.552,4.714L7.561,0.652C7.674,0.408 7.82,0.286 7.999,0.286C8.177,0.286 8.323,0.408 8.436,0.652L10.445,4.714L14.927,5.366C15.261,5.42 15.427,5.557 15.427,5.777L15.428,5.777Z" />
      </g>
    </svg>
  );
}
