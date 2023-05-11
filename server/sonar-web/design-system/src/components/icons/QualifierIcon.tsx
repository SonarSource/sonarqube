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
import { useTheme } from '@emotion/react';
import { themeColor } from '../../helpers/theme';
import { DirectoryIcon } from './DirectoryIcon';
import { FileIcon } from './FileIcon';
import { IconProps } from './Icon';
import { ProjectIcon } from './ProjectIcon';
import { TestFileIcon } from './TestFileIcon';

interface Props extends IconProps {
  qualifier: string | null | undefined;
}

export function QualifierIcon({ qualifier, fill, ...iconProps }: Props) {
  const theme = useTheme();

  if (!qualifier) {
    return null;
  }

  const icon = {
    dir: <DirectoryIcon fill={fill ?? themeColor('iconDirectory')({ theme })} {...iconProps} />,
    fil: <FileIcon fill={fill ?? themeColor('iconFile')({ theme })} {...iconProps} />,
    trk: <ProjectIcon fill={fill ?? themeColor('iconProject')({ theme })} {...iconProps} />,
    uts: <TestFileIcon fill={fill ?? themeColor('iconProject')({ theme })} {...iconProps} />,
  }[qualifier.toLowerCase()];

  return icon ?? null;
}
