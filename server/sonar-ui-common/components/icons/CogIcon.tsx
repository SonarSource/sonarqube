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

export default function CogIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  return (
    <Icon {...iconProps}>
      <path
        d="M14.922 9.704L13.6 8.696a4.55 4.551 0 000-1.057l1.323-1.006a.62.62 0 00.156-.805l-1.374-2.314a.658.658 0 00-.795-.28l-1.558.611a5.275 5.275 0 00-.935-.53l-.24-1.611a.631.631 0 00-.635-.537H6.787a.63.63 0 00-.633.532l-.239 1.616a5.62 5.62 0 00-.934.53l-1.563-.611a.645.645 0 00-.789.273L1.253 5.826a.616.616 0 00.157.808L2.73 7.64a4.517 4.519 0 000 1.058L1.41 9.705a.62.62 0 00-.158.805l1.374 2.314a.658.658 0 00.794.28l1.557-.61c.293.206.607.384.937.53l.24 1.61a.63.63 0 00.632.537H9.54a.63.63 0 00.634-.532l.24-1.616a5.62 5.62 0 00.934-.53l1.563.611a.645.645 0 00.789-.273l1.382-2.328a.618.619 0 00-.16-.8zm-6.758 1.382C6.51 11.087 5.17 9.78 5.17 8.17S6.51 5.252 8.164 5.252c1.654 0 2.995 1.307 2.995 2.917-.001 1.61-1.342 2.915-2.995 2.917z"
        fill={fill}
      />
    </Icon>
  );
}
