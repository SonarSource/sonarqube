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

export default function SecurityHotspotIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <g fill="none" fillRule="evenodd">
        <path
          d="M10.2764 2.3205c-.437-.905-1.273-1.521-2.227-1.521-1.402 0-2.549 1.333-2.549 2.959v5.541"
          stroke={fill}
          strokeLinecap="round"
          strokeWidth="1.14"
        />
        <path
          d="M5.2227 5.0215h5.555c1.222 0 2.222 1 2.222 2.222v4.444c0 1.223-1 2.223-2.222 2.223h-5.555c-1.223 0-2.223-1-2.223-2.223v-4.444c0-1.222 1-2.222 2.223-2.222zm2.15279 5.73895h1.25683c.00586-.22266.03663-.4065.09229-.55151.05566-.14502.15527-.28638.29883-.42408l.50537-.47021c.21387-.20801.36914-.41162.46582-.61084.09668-.19922.14502-.42041.14502-.66358 0-.55664-.17944-.98583-.53833-1.2876C9.24243 6.45089 8.73633 6.3 8.083 6.3c-.65626 0-1.16602.16333-1.5293.48999-.36328.32666-.54785.78296-.55371 1.3689h1.48535c.00586-.21973.06299-.39405.17139-.52295.1084-.1289.25049-.19336.42627-.19336.38086 0 .57129.22119.57129.66357 0 .18164-.0564.3479-.1692.49878-.11279.15088-.27758.31714-.49438.49878-.2168.18164-.37353.39624-.47021.6438-.09668.24756-.14502.5852-.14502 1.01294zm-.18018 1.33594c0 .2168.07837.39477.23511.53393.15674.13916.3523.20874.58667.20874.23438 0 .42993-.06958.58667-.20874.15674-.13916.2351-.31714.2351-.53393 0-.2168-.07836-.39478-.2351-.53394-.15674-.13916-.3523-.20874-.58667-.20874-.23438 0-.42993.06958-.58667.20874-.15674.13916-.2351.31714-.2351.53394z"
          style={{ fill }}
        />
      </g>
    </Icon>
  );
}
