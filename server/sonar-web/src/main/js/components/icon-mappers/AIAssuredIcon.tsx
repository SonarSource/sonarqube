/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { SheildCheckIcon } from '../ui/icon/SheildCheckIcon';
import { SheildCrossIcon } from '../ui/icon/SheildCrossIcon';
import { ShieldIcon } from '../ui/icon/ShieldIcon';

export enum AiIconColor {
  Disable = '--echoes-color-icon-disabled',
  Default = '--echoes-color-icon-default',
  Accent = '--echoes-color-icon-accent',
  Subdued = '--echoes-color-icon-subdued',
}

export enum AiIconVariant {
  Default,
  Check,
  Cross,
}

interface Props {
  className?: string;
  color?: AiIconColor;
  height?: number;
  variant?: AiIconVariant;
  width?: number;
}

const VariantComp = {
  [AiIconVariant.Check]: SheildCheckIcon,
  [AiIconVariant.Default]: ShieldIcon,
  [AiIconVariant.Cross]: SheildCrossIcon,
};

export default function AIAssuredIcon({
  color = AiIconColor.Accent,
  variant = AiIconVariant.Default,
  className,
  width = 20,
  height = 20,
}: Readonly<Props>) {
  const Comp = VariantComp[variant];
  return <Comp className={className} height={height} fill={`var(${color})`} width={width} />;
}
