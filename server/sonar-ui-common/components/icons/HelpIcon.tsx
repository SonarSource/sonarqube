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

interface Props extends IconProps {
  fillInner?: string;
}

export default function HelpIcon({ fill = 'currentColor', fillInner, ...iconProps }: Props) {
  return (
    <Icon {...iconProps}>
      <path
        d="M9.167 12.375v-1.75a.284.284 0 00-.082-.21.284.284 0 00-.21-.082h-1.75a.284.284 0 00-.21.082.284.284 0 00-.082.21v1.75c0 .085.028.155.082.21a.284.284 0 00.21.082h1.75a.284.284 0 00.21-.082.284.284 0 00.082-.21zM11.5 6.25c0-.535-.169-1.03-.506-1.486a3.452 3.452 0 00-1.262-1.057 3.462 3.462 0 00-1.55-.374c-1.476 0-2.603.647-3.381 1.942-.091.146-.067.273.073.383l1.203.911c.042.036.1.055.173.055a.269.269 0 00.228-.11c.322-.413.583-.692.784-.838.206-.146.468-.219.784-.219.291 0 .551.079.779.237.228.158.342.337.342.538 0 .23-.061.416-.183.556-.121.14-.328.276-.62.41a3.13 3.13 0 00-1.052.788c-.32.356-.479.737-.479 1.144v.328c0 .085.028.155.082.21a.284.284 0 00.21.082h1.75a.284.284 0 00.21-.082.284.284 0 00.082-.21c0-.115.065-.266.196-.45a1.54 1.54 0 01.496-.452c.195-.11.344-.196.447-.26a3.84 3.84 0 00.42-.319c.175-.149.31-.294.405-.437a2.407 2.407 0 00.369-1.29zM15 8c0 1.27-.313 2.441-.939 3.514a6.969 6.969 0 01-2.547 2.547A6.848 6.848 0 018 15a6.848 6.848 0 01-3.514-.939 6.969 6.969 0 01-2.547-2.547A6.848 6.848 0 011 8c0-1.27.313-2.441.939-3.514A6.969 6.969 0 014.486 1.94 6.848 6.848 0 018 1c1.27 0 2.441.313 3.514.939a6.969 6.969 0 012.547 2.547A6.848 6.848 0 0115 8z"
        fill={fill}
      />
      {fillInner && (
        <path
          d="M9.167 12.375v-1.75a.284.284 0 00-.082-.21.284.284 0 00-.21-.082h-1.75a.284.284 0 00-.21.082.284.284 0 00-.082.21v1.75c0 .085.028.155.082.21a.284.284 0 00.21.082h1.75a.284.284 0 00.21-.082.284.284 0 00.082-.21zM11.5 6.25c0-.535-.169-1.03-.506-1.486a3.452 3.452 0 00-1.262-1.057 3.462 3.462 0 00-1.55-.374c-1.476 0-2.603.647-3.381 1.942-.091.146-.067.273.073.383l1.203.911c.042.036.1.055.173.055a.269.269 0 00.228-.11c.322-.413.583-.692.784-.838.206-.146.468-.219.784-.219.291 0 .551.079.779.237.228.158.342.337.342.538 0 .23-.061.416-.183.556-.121.14-.328.276-.62.41a3.13 3.13 0 00-1.052.788c-.32.356-.479.737-.479 1.144v.328c0 .085.028.155.082.21a.284.284 0 00.21.082h1.75a.284.284 0 00.21-.082.284.284 0 00.082-.21c0-.115.065-.266.196-.45a1.54 1.54 0 01.496-.452c.195-.11.344-.196.447-.26a3.84 3.84 0 00.42-.319c.175-.149.31-.294.405-.437a2.407 2.407 0 00.369-1.29z"
          fill={fillInner}
        />
      )}
    </Icon>
  );
}
