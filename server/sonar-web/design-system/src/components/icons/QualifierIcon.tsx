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
import { useTheme } from '@emotion/react';
import { themeColor } from '../../helpers/theme';
import { DirectoryIcon } from './DirectoryIcon';
import { FileIcon } from './FileIcon';
import { CustomIcon, IconProps } from './Icon';
import { ProjectIcon } from './ProjectIcon';
import { TestFileIcon } from './TestFileIcon';

interface Props extends IconProps {
  qualifier: string | null | undefined;
}

const defaultIconfill = 'var(--echoes-color-icon-subdued)';

export function QualifierIcon({ qualifier, fill, ...iconProps }: Readonly<Props>) {
  const theme = useTheme();

  if (!qualifier) {
    return null;
  }

  const icon = {
    app: ApplicationIcon({ fill: fill ?? defaultIconfill, ...iconProps }),
    dir: <DirectoryIcon fill={fill ?? themeColor('iconDirectory')({ theme })} {...iconProps} />,
    fil: <FileIcon fill={fill ?? defaultIconfill} {...iconProps} />,
    svw: SubPortfolioIcon({ fill: fill ?? defaultIconfill, ...iconProps }),
    trk: <ProjectIcon fill={fill ?? defaultIconfill} {...iconProps} />,
    uts: <TestFileIcon fill={fill ?? defaultIconfill} {...iconProps} />,
    vw: PortfolioIcon({ fill: fill ?? defaultIconfill, ...iconProps }),
  }[qualifier.toLowerCase()];

  return icon ?? null;
}

function PortfolioIcon({ fill = 'currentColor', ...iconProps }: Readonly<IconProps>) {
  const theme = useTheme();

  return (
    <CustomIcon {...iconProps}>
      <path
        d="M14.97 14.97H1.016V1.015H14.97V14.97zm-1-12.955H2.015V13.97H13.97V2.015zm-.973 10.982H9V9h3.997v3.997zM7 12.996H3.004V9H7v3.996zM11.997 10H10v1.997h1.997V10zM6 10H4.004v1.996H6V10zm1-3H3.006V3.006H7V7zm5.985 0H9V3.015h3.985V7zM6 4.006H4.006V6H6V4.006zm5.985.009H10V6h1.985V4.015z"
        fill={themeColor(fill)({ theme })}
      />
    </CustomIcon>
  );
}

function ApplicationIcon({ fill = 'currentColor', ...iconProps }: Readonly<IconProps>) {
  const theme = useTheme();

  return (
    <CustomIcon {...iconProps}>
      <path
        d="M3.014 10.986a2 2 0 1 1-.001 4.001 2 2 0 0 1 .001-4.001zm9.984 0a2 2 0 1 1-.001 4.001 2 2 0 0 1 .001-4.001zm-5.004-.021c1.103 0 2 .896 2 2s-.897 2-2 2a2 2 0 0 1 0-4zm-4.98 1.021a1 1 0 1 1 0 2 1 1 0 0 1 0-2zm9.984 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2zm-5.004-.021a1 1 0 1 1 0 2 1 1 0 0 1 0-2zM2.984 6a2 2 0 1 1-.001 4.001A2 2 0 0 1 2.984 6zm9.984 0a2 2 0 1 1-.001 4.001A2 2 0 0 1 12.968 6zm-5.004-.021c1.103 0 2 .897 2 2a2 2 0 1 1-2-2zM2.984 7a1 1 0 1 1 0 2 1 1 0 0 1 0-2zm9.984 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2zm-5.004-.021a1.001 1.001 0 0 1 0 2 1 1 0 0 1 0-2zM3 1.025a2 2 0 1 1-.001 4.001A2 2 0 0 1 3 1.025zm9.984 0a2 2 0 1 1-.001 4.001 2 2 0 0 1 .001-4.001zM7.98 1.004c1.103 0 2 .896 2 2s-.897 2-2 2a2 2 0 0 1 0-4zM3 2.025a1 1 0 1 1 0 2 1 1 0 0 1 0-2zm9.984 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2zM7.98 2.004a1.001 1.001 0 0 1 0 2 1 1 0 0 1 0-2z"
        fill={themeColor(fill)({ theme })}
      />
    </CustomIcon>
  );
}

function SubPortfolioIcon({ fill = 'currentColor', ...iconProps }: Readonly<IconProps>) {
  const theme = useTheme();

  return (
    <CustomIcon {...iconProps}>
      <path
        d="M14 7h2v9H7v-2H0V0h14v7zM8 8v7h7V8H8zm3 6H9v-2h2v2zm3 0h-2v-2h2v2zm-1-7V1H1v12h6V7h6zm-7 5H2V8h4v4zm5-1H9V9h2v2zm3 0h-2V9h2v2zM5 9H3v2h2V9zm1-3H2V2h4v4zm6 0H8V2h4v4zM5 3H3v2h2V3zm6 0H9v2h2V3z"
        fill={themeColor(fill)({ theme })}
      />
    </CustomIcon>
  );
}
