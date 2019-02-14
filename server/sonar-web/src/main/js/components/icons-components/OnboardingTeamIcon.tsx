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

export default function OnboardingTeamIcon({ className, fill = theme.darkBlue, size }: IconProps) {
  return (
    <Icon className={className} size={size || 64} viewBox="0 0 64 64">
      <g fill="none" fillRule="evenodd" stroke={fill} strokeWidth="2">
        <path d="M32 9v5M11.5195 43.0898l7.48-4.091m33.481-18.0994l-7.48 4.1m-33.481-4.1l7.48 4.1M45 38.999l7.48 4.101M32 50v5m15-23c0 8.284-6.715 15-15 15s-15-6.716-15-15c0-8.285 6.715-15 15-15s15 6.715 15 15z" />
        <path d="M40 38c0 1.656-3.58 2-8 2s-8-.344-8-2m16 0v-3l-5-3-1-1m-10 7v-3l5-3 1-1m6-4c0 2.2-1.8 4-4 4s-4-1.8-4-4v-1c0-2.2 1.8-4 4-4s4 1.8 4 4v1zm-.0098-21.71c7.18 1.069 13.439 4.96 17.609 10.51m-17.609 42.91c7.18-1.07 13.439-4.96 17.609-10.51M6.6299 41.25c-1.06-2.88-1.63-6-1.63-9.25s.57-6.37 1.63-9.25m3.7705-6.9502c4.17-5.55 10.43-9.44 17.609-10.51m-17.609 42.9104c4.17 5.55 10.43 9.439 17.609 10.51M57.3701 22.75c1.06 2.88 1.63 6 1.63 9.25s-.57 6.37-1.63 9.25" />
        <path d="M36 5c0 2.209-1.79 4-4 4-2.209 0-4-1.791-4-4 0-2.21 1.791-4 4-4 2.21 0 4 1.79 4 4zm-5 0h2M12 19c0 2.209-1.79 4-4 4-2.209 0-4-1.791-4-4 0-2.21 1.791-4 4-4 2.21 0 4 1.79 4 4zm-5 0h2m51 0c0 2.209-1.79 4-4 4-2.209 0-4-1.791-4-4 0-2.21 1.791-4 4-4 2.21 0 4 1.79 4 4zm-5 0h2M12 45c0 2.209-1.79 4-4 4-2.209 0-4-1.791-4-4 0-2.21 1.791-4 4-4 2.21 0 4 1.79 4 4zm-5 0h2m51 0c0 2.209-1.79 4-4 4-2.209 0-4-1.791-4-4 0-2.21 1.791-4 4-4 2.21 0 4 1.79 4 4zm-5 0h2M36 59c0 2.209-1.79 4-4 4-2.209 0-4-1.791-4-4 0-2.21 1.791-4 4-4 2.21 0 4 1.79 4 4zm-5 0h2" />
      </g>
    </Icon>
  );
}
