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

export default function InfoIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M10.333 12.375v-1.458a.288.288 0 0 0-.291-.292h-.875V5.958a.288.288 0 0 0-.292-.291H5.958a.288.288 0 0 0-.291.291v1.459c0 .164.127.291.291.291h.875v2.917h-.875a.288.288 0 0 0-.291.292v1.458c0 .164.127.292.291.292h4.084a.288.288 0 0 0 .291-.292zM9.167 4.208V2.75a.288.288 0 0 0-.292-.292h-1.75a.288.288 0 0 0-.292.292v1.458c0 .164.128.292.292.292h1.75a.288.288 0 0 0 .292-.292zM15 8c0 3.865-3.135 7-7 7s-7-3.135-7-7 3.135-7 7-7 7 3.135 7 7z"
        style={{ fill }}
      />
    </Icon>
  );
}
