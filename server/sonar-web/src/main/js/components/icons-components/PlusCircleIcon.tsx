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

export default function PlusCircleIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M8 1c3.863 0 7 3.137 7 7s-3.137 7-7 7-7-3.137-7-7 3.137-7 7-7zm3.726 7.985A.274.274 0 0 0 12 8.711V7.289a.274.274 0 0 0-.274-.274H8.985V4.274A.274.274 0 0 0 8.711 4H7.289a.274.274 0 0 0-.274.274v2.741H4.274A.274.274 0 0 0 4 7.289v1.422c0 .152.123.274.274.274h2.741v2.741c0 .151.122.274.274.274h1.422a.274.274 0 0 0 .274-.274V8.985h2.741z"
        style={{ fill }}
      />
    </Icon>
  );
}
