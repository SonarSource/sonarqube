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
import { blue } from '../../app/theme';

interface Props extends IconProps {
  hasUnread?: boolean;
}

export default function NotificationIcon({
  className,
  fill = 'currentColor',
  hasUnread,
  size
}: Props) {
  return (
    <Icon className={className} size={size}>
      {hasUnread ? (
        <>
          <path
            d="M8 1a.875.875 0 0 0-.875.875v.57c-2.009.418-3.498 2.118-3.498 4.242 0 2.798-.987 3.652-1.516 4.22a.856.856 0 0 0-.236.593.875.875 0 0 0 .877.875h10.496a.875.875 0 0 0 .877-.875.854.854 0 0 0-.236-.594c-.497-.534-1.388-1.342-1.494-3.76a2.814 2.814 0 0 1-.768.108A2.814 2.814 0 0 1 8.814 4.44a2.814 2.814 0 0 1 .665-1.818 4.543 4.543 0 0 0-.604-.178v-.57A.875.875 0 0 0 8 1zM6.25 13.25a1.75 1.75 0 0 0 3.5 0h-3.5z"
            style={{ fill }}
          />
          <circle cx="11.627" cy="4.441" r="2" style={{ fill: blue }} />
        </>
      ) : (
        <path
          d="M8 15a1.75 1.75 0 0 0 1.75-1.75h-3.5c0 .967.784 1.75 1.75 1.75zm5.89-4.094c-.529-.567-1.517-1.421-1.517-4.218 0-2.125-1.49-3.826-3.499-4.243v-.57a.875.875 0 1 0-1.748 0v.57c-2.01.417-3.499 2.118-3.499 4.243 0 2.797-.988 3.65-1.517 4.218a.854.854 0 0 0-.235.594.876.876 0 0 0 .878.875h10.494a.876.876 0 0 0 .878-.875.853.853 0 0 0-.235-.594z"
          style={{ fill }}
        />
      )}
    </Icon>
  );
}
