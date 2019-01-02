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

export default function CollapseIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M8 8.509v3.56c0 .138-.05.257-.151.357-.1.101-.22.151-.358.151a.489.489 0 0 1-.357-.15l-1.145-1.145-2.638 2.639a.251.251 0 0 1-.366 0l-.906-.906a.251.251 0 0 1 0-.366l2.639-2.638-1.144-1.145a.489.489 0 0 1-.151-.357c0-.138.05-.257.15-.358.101-.1.22-.151.358-.151h3.56c.138 0 .257.05.358.151.1.1.151.22.151.358zm6-5.34c0 .068-.026.129-.08.182l-2.638 2.638 1.144 1.145c.101.1.151.22.151.357 0 .138-.05.257-.15.358-.101.1-.22.151-.358.151h-3.56a.489.489 0 0 1-.358-.151A.489.489 0 0 1 8 7.491v-3.56c0-.138.05-.257.151-.357.1-.101.22-.151.358-.151.137 0 .257.05.357.15l1.145 1.145 2.638-2.639a.251.251 0 0 1 .366 0l.906.906c.053.053.079.114.079.183z"
        style={{ fill }}
      />
    </Icon>
  );
}
