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

export default function RocketIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  return (
    <Icon {...iconProps}>
      <path
        d="M13.754 2.002C11.41 1.96 8.74 3.184 7.049 5.084A6.345 6.345 0 002.7 6.935a.25.25 0 00.14.426l1.927.276-.238.266a.25.25 0 00.01.344l3.213 3.213a.25.25 0 00.344.01l.266-.239.276 1.928c.014.093.088.162.177.192a.23.23 0 00.072.011.282.282 0 00.193-.08 6.331 6.331 0 001.836-4.332c1.901-1.694 3.136-4.365 3.081-6.704a.251.251 0 00-.244-.244zM11.45 6.318a1.246 1.246 0 01-.884.365c-.32 0-.64-.122-.884-.365a1.252 1.252 0 010-1.768 1.251 1.251 0 011.768 0 1.251 1.251 0 010 1.768zm-8.088 4.135c-.535.535-1.27 2.952-1.351 3.225a.25.25 0 00.311.311c.274-.082 2.69-.816 3.226-1.351a1.547 1.547 0 000-2.185 1.548 1.548 0 00-2.186 0z"
        style={{ fill }}
      />
    </Icon>
  );
}
