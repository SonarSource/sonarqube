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

export default function BugIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M8.01 10.9885h1v-5h-1v5zm3-2h1.265l.46.771.775-.543-.733-1.228H11.01v-.316l2-2.343v-2.341h-1v1.972l-1 1.172v-1.144h-.029c-.101-.826-.658-1.52-1.436-1.853l1.472-1.349-.676-.736-1.831 1.678-1.831-1.678-.676.736 1.472 1.349c-.778.333-1.335 1.027-1.436 1.853H6.01v1.144l-1-1.172v-1.972h-1v2.341l2 2.343v.316H4.243l-.733 1.228.775.543.46-.771H6.01v.287l-2 1.912v2.801h1v-2.374l1.003-.959c.018 1.289 1.07 2.333 2.363 2.333h.768c.741 0 1.418-.347 1.767-.907.258-.411.304-.887.16-1.365l.939.898v2.374h1v-2.801l-2-1.912v-.287z"
        fillRule="evenodd"
        style={{ fill }}
      />
    </Icon>
  );
}
