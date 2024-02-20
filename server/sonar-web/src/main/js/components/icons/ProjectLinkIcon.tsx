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
import {
  FileIcon,
  HomeIcon,
  LinkExternalIcon,
  IconProps as MIUIIconProps,
  PulseIcon,
  SyncIcon,
} from '@primer/octicons-react';
import React, { FC } from 'react';

interface ProjectLinkIconProps {
  type: string;
}

export default function ProjectLinkIcon({ type, ...iconProps }: ProjectLinkIconProps) {
  const getIcon = (): FC<React.PropsWithChildren<MIUIIconProps>> => {
    switch (type) {
      case 'issue':
        return PulseIcon;
      case 'homepage':
        return HomeIcon;
      case 'ci':
        return SyncIcon;
      case 'scm':
        return FileIcon;
      default:
        return LinkExternalIcon;
    }
  };

  const Icon = getIcon();

  return <Icon {...iconProps} />;
}
