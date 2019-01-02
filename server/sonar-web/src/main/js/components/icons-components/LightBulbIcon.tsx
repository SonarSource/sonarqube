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

export default function LightBulbIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M10.042 5.083a.3.3 0 0 1-.292.292.3.3 0 0 1-.292-.292c0-.629-.975-.875-1.458-.875a.3.3 0 0 1-.292-.291A.3.3 0 0 1 8 3.625c.848 0 2.042.447 2.042 1.458zm1.458 0c0-1.823-1.85-2.916-3.5-2.916S4.5 3.26 4.5 5.083c0 .584.237 1.194.62 1.641.173.2.373.392.556.602.647.774 1.194 1.686 1.285 2.716h2.078c.091-1.03.638-1.942 1.285-2.716.183-.21.383-.402.556-.602.383-.447.62-1.057.62-1.64zm1.167 0c0 .94-.31 1.75-.94 2.443-.628.693-1.457 1.668-1.53 2.643a.876.876 0 0 1 .428.748.852.852 0 0 1-.228.583.852.852 0 0 1 .228.583c0 .301-.155.575-.41.739a.89.89 0 0 1 .118.428c0 .592-.465.875-.993.875A1.479 1.479 0 0 1 8 15a1.479 1.479 0 0 1-1.34-.875c-.528 0-.993-.283-.993-.875 0-.146.045-.3.118-.428a.876.876 0 0 1-.41-.739c0-.218.082-.428.228-.583a.852.852 0 0 1-.228-.583c0-.301.164-.593.428-.748-.073-.975-.902-1.95-1.53-2.643a3.507 3.507 0 0 1-.94-2.443C3.333 2.604 5.694 1 8 1c2.306 0 4.667 1.604 4.667 4.083z"
        style={{ fill }}
      />
    </Icon>
  );
}
