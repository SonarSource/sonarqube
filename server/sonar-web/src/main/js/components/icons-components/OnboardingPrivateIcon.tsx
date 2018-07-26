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
import Icon, { IconProps } from './Icon';

export default function OnboardingPrivateIcon({
  className,
  fill = 'currentColor',
  size
}: IconProps) {
  return (
    <Icon className={className} size={size || 64} viewBox="0 0 64 64">
      <g fill="none" stroke={fill} strokeWidth="2">
        <path d="M2 59h60V13H2zm0-46h60V5H2zm3-4h2m2 0h2m2 0h2m2 0h42" />
        <path d="M59 34h-6l-2-4h-6l-2 5h-6l-2 2h-6l-2-4h-6l-2 5h-6l-2 4H5m1 14v-9m4 9v-6m4 6V43m4 13V45m4 11V42m4 14V39m4 17V41m4 15V46m4 10V40m4 16V44m4 12V37m4 19V38m4 18V43m4 13V39m-3-18h-2m-2 0h-2m-2 0h-2M9 29h14M9 33h7m17-12h8m-14 4h8m-8-4h4m-21 4h12v-4H10z" />
        <path d="M58 31V17H6v22" />
        <path
          d="M50 36c0-9.389-7.611-17-17-17s-17 7.611-17 17 7.611 17 17 17 17-7.611 17-17"
          fill="#FFF"
          stroke="none"
        />
        <path d="M50 36c0-9.389-7.611-17-17-17s-17 7.611-17 17 7.611 17 17 17 17-7.611 17-17z" />
        <mask fill="#FFF" id="a">
          <path d="M0 56h62V0H0z" />
        </mask>
        <path
          d="M27 45h12V33H27zm10-12v-4c0-1.023-.391-2.047-1.172-2.828C35.048 25.391 34.023 25 33 25s-2.048.391-2.828 1.172C29.391 26.953 29 27.977 29 29v4"
          mask="url(#a)"
        />
      </g>
    </Icon>
  );
}
