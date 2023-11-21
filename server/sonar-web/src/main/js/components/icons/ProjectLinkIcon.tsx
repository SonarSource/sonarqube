/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import BugTrackerIcon from './BugTrackerIcon';
import ContinuousIntegrationIcon from './ContinuousIntegrationIcon';
import DetachIcon from './DetachIcon';
import HouseIcon from './HouseIcon';
import { IconProps } from './Icon';
import SCMIcon from './SCMIcon';

interface ProjectLinkIconProps {
  type: string;
  miui?: boolean;
}

export default function ProjectLinkIcon({
  miui,
  type,
  ...iconProps
}: IconProps & ProjectLinkIconProps) {
  const getIcon = (): FC<
    React.PropsWithChildren<React.PropsWithChildren<IconProps | MIUIIconProps>>
  > => {
    switch (type) {
      case 'issue':
        return miui ? PulseIcon : BugTrackerIcon;
      case 'homepage':
        return miui ? HomeIcon : HouseIcon;
      case 'ci':
        return miui ? SyncIcon : ContinuousIntegrationIcon;
      case 'scm':
        return miui ? FileIcon : SCMIcon;
      default:
        return miui ? LinkExternalIcon : DetachIcon;
    }
  };

  const Icon = getIcon();

  return <Icon {...iconProps} />;
}
