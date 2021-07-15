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

export default function DeleteIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  return (
    <Icon {...iconProps}>
      <path
        d="M13.571429 1.8750019h-3.214285l-.251787-.5113283a.64285716.65624976 0 0 0-.5758927-.3636718H6.4678572a.63535718.64859353 0 0 0-.5732142.3636718l-.2517858.5113283H2.4285714A.42857144.43749984 0 0 0 2 2.3125018v.8749996a.42857144.43749984 0 0 0 .4285714.4374999H13.571429A.42857144.43749984 0 0 0 14 3.1875014v-.8749996a.42857144.43749984 0 0 0-.428571-.4374999zM3.4250001 13.769529a1.2857144 1.3124996 0 0 0 1.2830357 1.230468h6.5839282A1.2857144 1.3124996 0 0 0 12.575 13.769529l.567857-9.269528H2.8571428z"
        style={{ fill }}
      />
    </Icon>
  );
}
