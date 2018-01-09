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
import { IconProps } from './types';

export default function DropdownIcon({ className, fill = 'currentColor', size = 16 }: IconProps) {
  return (
    <svg
      className={className}
      width={size / 16 * 7}
      height={size}
      viewBox="0 0 7 16"
      version="1.1"
      xmlnsXlink="http://www.w3.org/1999/xlink"
      xmlSpace="preserve">
      <g transform="matrix(0.0273438,0,0,0.0273438,-6.4e-06,2.65625)">
        <path
          style={{ fill }}
          d="M256,176C256,180.333 254.417,184.083 251.25,187.25L139.25,299.25C136.083,302.417 132.333,304 128,304C123.667,304 119.917,302.417 116.75,299.25L4.75,187.25C1.583,184.083 0,180.333 0,176C0,171.667 1.583,167.917 4.75,164.75C7.917,161.583 11.667,160 16,160L240,160C244.333,160 248.083,161.583 251.25,164.75C254.417,167.917 256,171.667 256,176Z"
        />
      </g>
    </svg>
  );
}
