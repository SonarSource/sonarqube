/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as theme from '../../app/theme';

export default function OnboardingProjectIcon({
  className,
  fill = theme.darkBlue,
  size
}: IconProps) {
  return (
    <Icon className={className} size={size || 64} viewBox="0 0 64 64">
      <g fill="none" fillRule="evenodd" stroke={fill} strokeWidth="2">
        <path d="M2 59h60V13H2zm0-46h60V5H2zm3-4h2m2 0h2m2 0h2m2 0h42" />
        <path d="M59 34h-6l-2-4h-6l-2 5h-6l-2 2h-6l-2-4h-6l-2 5h-6l-2 4H5m1 14v-9m4 9v-6m4 6V43m4 13V45m4 11V42m4 14V39m4 17V41m4 15V46m4 10V40m4 16V44m4 12V37m4 19V38m4 18V43m4 13V39m-3-18h-2m-2 0h-2m-2 0h-2M9 29h14M9 33h7m17-12h8m-14 4h8m-8-4h4m-21 4h12v-4H10z" />
        <path d="M58 31V17H6v22" />
      </g>
    </Icon>
  );
}
