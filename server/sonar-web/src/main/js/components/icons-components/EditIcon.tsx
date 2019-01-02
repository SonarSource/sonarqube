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

export default function EditIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M4.875 12.986l.721-.72-1.861-1.862-.721.72v.848h1.014v1.014h.847zm4.143-7.35c0-.117-.058-.175-.174-.175a.183.183 0 0 0-.135.056L4.416 9.81a.183.183 0 0 0-.056.135c0 .116.058.174.175.174a.183.183 0 0 0 .134-.056L8.962 5.77a.183.183 0 0 0 .056-.134zM8.59 4.115l3.295 3.295L5.295 14H2v-3.295l6.59-6.59zm5.41.76a.97.97 0 0 1-.293.713l-1.315 1.315-3.295-3.295L10.412 2.3c.19-.2.428-.301.713-.301.28 0 .52.1.72.301l1.862 1.853c.195.206.293.447.293.721z"
        style={{ fill }}
      />
    </Icon>
  );
}
