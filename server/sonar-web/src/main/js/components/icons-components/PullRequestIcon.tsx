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
import * as theme from '../../app/theme';

export default function PullRequestIcon({ className, fill = theme.blue, size = 16 }: IconProps) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 16 16"
      version="1.1"
      xmlnsXlink="http://www.w3.org/1999/xlink"
      xmlSpace="preserve">
      <path
        style={{ fill }}
        d="M3 11.9V4.1c.9-.2 1.5-1 1.5-1.9 0-1.1-.9-2-2-2s-2 .9-2 2c0 .9.6 1.7 1.5 1.9v7.8c-.9.2-1.5 1-1.5 1.9 0 1.1.9 2 2 2s2-.9 2-2c0-.9-.6-1.6-1.5-1.9zM1.5 2.2c0-.6.4-1 1-1s1 .4 1 1-.4 1-1 1-1-.4-1-1zm1 12.7c-.6 0-1-.4-1-1s.4-1 1-1 1 .4 1 1-.4 1-1 1zM14 11.9V5.5c0-.1-.2-3.1-5.1-3.5L10.1.8 9.5.1 6.9 2.6l2.6 2.5.7-.7L8.8 3c4 .2 4.2 2.4 4.2 2.5v6.4c-.9.2-1.5 1-1.5 1.9 0 1.1.9 2 2 2s2-.9 2-2c0-.9-.6-1.6-1.5-1.9zm-.5 3c-.6 0-1-.4-1-1s.4-1 1-1 1 .4 1 1-.4 1-1 1z"
      />
    </svg>
  );
}
