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
import * as theme from '../../app/theme';

export default function OnboardingAddMembersIcon({
  className,
  fill = theme.darkBlue,
  size = 64
}: IconProps) {
  return (
    <Icon className={className} height={(size / 64) * 80} viewBox="0 0 64 80" width={size}>
      <path
        d="M49 34c0 9.389-7.611 17-17 17s-17-7.611-17-17 7.611-17 17-17 17 7.611 17 17z"
        style={{ fill: 'none', stroke: fill, strokeWidth: 2 }}
      />
      <path
        d="M36 32c0 2.2-1.8 4-4 4s-4-1.8-4-4v-1c0-2.2 1.8-4 4-4s4 1.8 4 4v1zm4 39a8 8 0 1 1-16 0 8 8 0 0 1 16 0z"
        style={{ fill: 'none', stroke: fill, strokeWidth: 2 }}
      />
      <path
        d="M33 70h2v2h-2v2h-2v-2h-2v-2h2v-2h2v2zm-5-14l-.072-.001c-1.521-.054-2.834-1.337-2.925-2.855L25 50h2c0 1.745-.532 3.91.952 3.999L28 54h8v.002l.072-.005c.506-.042.922-.489.928-1.003V50h2c0 1.024.011 2.048-.001 3.072-.054 1.518-1.337 2.834-2.855 2.925l-.072.002L36 56v8h-2v-7.982c-1.333.007-2.667.007-4 0V64h-2v-8zm-7 0H1V10 0h62v56H43v-2h18V10H3v44h18v2zm38-4H43v-2h14V14H7v36h14v2H5V12h54v40zm-19-9l1 .017c-.03 1.79-2.454 2.506-3.918 2.717-4.074.584-8.503.911-12.176-.477-.949-.358-1.887-1.119-1.906-2.24l.191-.017H23v-3.566l5.38-3.228.913-.913 1.414 1.414-1.087 1.087L25 40.566v2.438c.067 1.304 10.98 2.117 13.844.157.076-.052.152-.172.156-.178v-2.417l-4.62-2.772-1.087-1.087 1.414-1.414.913.913L41 39.434V43h-1zm14-4h-2v-2h2v2zm-42 0h-2v-2h2v2zm42-4h-2v-2h2v2zm-42 0h-2v-2h2v2zm42-4h-2v-2h2v2zm-42 0h-2v-2h2v2zm20.198-10.999c3.529.062 6.837 1.669 9.386 4.169l-1.289 1.539c-4.178-4.152-11.167-5.254-16.359-.228l-.231.228-1.41-1.418c2.633-2.617 6.031-4.313 9.903-4.29zM3 2v6h58V2H3zm56 4H17V4h42v2zM11 6H9V4h2v2zM7 6H5V4h2v2zm8 0h-2V4h2v2z"
        style={{ fill, fillRule: 'nonzero' }}
      />
    </Icon>
  );
}
