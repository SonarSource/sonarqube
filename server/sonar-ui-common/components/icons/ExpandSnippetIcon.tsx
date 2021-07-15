/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import Icon, { IconProps } from './Icon';

export default function ExpandSnippetIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  return (
    <Icon {...iconProps}>
      <g fill="none" fillRule="evenodd">
        <path
          d="M8 1v4H4"
          stroke={fill}
          strokeWidth="2"
          transform="scale(-.83333 -.84583) rotate(45 7.66 -19.75)"
        />
        <path d="M3 5.78h10v1.7H3z" fill={fill} />
        <path d="M7.17 2.4h1.66v5.07H7.17z" fill={fill} />
        <g>
          <path
            d="M8.16 1.81V6.1H3.9"
            stroke={fill}
            strokeWidth="2"
            transform="scale(.83333 .84583) rotate(45 -4.2 13.2)"
          />
          <path d="M13 10.01H3v-1.7h10z" fill={fill} />
          <path d="M8.83 13.4H7.17V9.15h1.66z" fill={fill} />
        </g>
      </g>
    </Icon>
  );
}
