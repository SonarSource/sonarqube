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
import { IconProps, ThemedIcon } from './Icon';

interface Props extends IconProps {
  status: string;
}

const statusIcons: T.Dict<(props: IconProps) => React.ReactElement> = {
  open: OpenStatusIcon,
  confirmed: ConfirmedStatusIcon,
  reopened: ReopenedStatusIcon,
  resolved: ResolvedStatusIcon,
  closed: ClosedStatusIcon,
  to_review: OpenStatusIcon,
  in_review: ConfirmedStatusIcon,
  reviewed: ResolvedStatusIcon,
};

export default function StatusIcon({ status, ...iconProps }: Props) {
  const Icon = statusIcons[status.toLowerCase()];
  return Icon ? <Icon {...iconProps} /> : null;
}

function OpenStatusIcon(iconProps: IconProps) {
  return (
    <ThemedIcon {...iconProps}>
      {({ theme }) => (
        <path
          d="M8 3.75c-.77 0-1.482.19-2.133.57A4.25 4.25 0 0 0 4.32 5.867c-.38.65-.57 1.362-.57 2.133 0 .77.19 1.482.57 2.133.38.65.896 1.167 1.547 1.547.65.38 1.362.57 2.133.57.77 0 1.482-.19 2.133-.57a4.242 4.242 0 0 0 1.547-1.547c.38-.65.57-1.362.57-2.133 0-.77-.19-1.482-.57-2.133a4.25 4.25 0 0 0-1.547-1.547A4.153 4.153 0 0 0 8 3.75zM14 8c0 1.09-.268 2.092-.805 3.012a5.96 5.96 0 0 1-2.183 2.183A5.863 5.863 0 0 1 8 14a5.863 5.863 0 0 1-3.012-.805 5.96 5.96 0 0 1-2.183-2.183A5.863 5.863 0 0 1 2 8c0-1.09.268-2.092.805-3.012a5.96 5.96 0 0 1 2.183-2.183A5.863 5.863 0 0 1 8 2c1.09 0 2.092.268 3.012.805a5.96 5.96 0 0 1 2.183 2.183C13.732 5.908 14 6.91 14 8z"
          style={{ fill: theme.colors.blue }}
        />
      )}
    </ThemedIcon>
  );
}

function ConfirmedStatusIcon(iconProps: IconProps) {
  return (
    <ThemedIcon {...iconProps}>
      {({ theme }) => (
        <path
          d="M10 8c0 .552-.195 1.023-.586 1.414-.39.39-.862.586-1.414.586a1.926 1.926 0 0 1-1.414-.586A1.928 1.928 0 0 1 6 8c0-.552.195-1.023.586-1.414C6.976 6.196 7.448 6 8 6c.552 0 1.023.195 1.414.586.39.39.586.862.586 1.414zM8 3.75c-.77 0-1.482.19-2.133.57A4.25 4.25 0 0 0 4.32 5.867c-.38.65-.57 1.362-.57 2.133 0 .77.19 1.482.57 2.133.38.65.896 1.167 1.547 1.547.65.38 1.362.57 2.133.57.77 0 1.482-.19 2.133-.57a4.242 4.242 0 0 0 1.547-1.547c.38-.65.57-1.362.57-2.133 0-.77-.19-1.482-.57-2.133a4.25 4.25 0 0 0-1.547-1.547A4.153 4.153 0 0 0 8 3.75zM14 8c0 1.09-.268 2.092-.805 3.012a5.96 5.96 0 0 1-2.183 2.183A5.863 5.863 0 0 1 8 14a5.863 5.863 0 0 1-3.012-.805 5.96 5.96 0 0 1-2.183-2.183A5.863 5.863 0 0 1 2 8c0-1.09.268-2.092.805-3.012a5.96 5.96 0 0 1 2.183-2.183A5.863 5.863 0 0 1 8 2c1.09 0 2.092.268 3.012.805a5.96 5.96 0 0 1 2.183 2.183C13.732 5.908 14 6.91 14 8z"
          style={{ fill: theme.colors.blue }}
        />
      )}
    </ThemedIcon>
  );
}

function ReopenedStatusIcon(iconProps: IconProps) {
  return (
    <ThemedIcon {...iconProps}>
      {({ theme }) => (
        <path
          d="M8 12.25v-8.5c-.77 0-1.482.19-2.133.57A4.25 4.25 0 0 0 4.32 5.867c-.38.65-.57 1.362-.57 2.133 0 .77.19 1.482.57 2.133.38.65.896 1.167 1.547 1.547.65.38 1.362.57 2.133.57zM14 8c0 1.09-.268 2.092-.805 3.012a5.96 5.96 0 0 1-2.183 2.183A5.863 5.863 0 0 1 8 14a5.863 5.863 0 0 1-3.012-.805 5.96 5.96 0 0 1-2.183-2.183A5.863 5.863 0 0 1 2 8c0-1.09.268-2.092.805-3.012a5.96 5.96 0 0 1 2.183-2.183A5.863 5.863 0 0 1 8 2c1.09 0 2.092.268 3.012.805a5.96 5.96 0 0 1 2.183 2.183C13.732 5.908 14 6.91 14 8z"
          style={{ fill: theme.colors.blue }}
        />
      )}
    </ThemedIcon>
  );
}

function ResolvedStatusIcon(iconProps: IconProps) {
  return (
    <ThemedIcon {...iconProps}>
      {({ theme }) => (
        <path
          d="M12.03 6.734a.49.49 0 0 0-.14-.36l-.71-.702a.48.48 0 0 0-.352-.15.474.474 0 0 0-.35.15l-3.19 3.18-1.765-1.766a.479.479 0 0 0-.35-.15.479.479 0 0 0-.353.15l-.71.703a.482.482 0 0 0-.14.358c0 .14.046.258.14.352l2.828 2.828c.098.1.216.15.35.15.142 0 .26-.05.36-.15l4.243-4.242a.475.475 0 0 0 .14-.352l-.001.001zM14 8c0 1.09-.268 2.092-.805 3.012a5.96 5.96 0 0 1-2.183 2.183A5.863 5.863 0 0 1 8 14a5.863 5.863 0 0 1-3.012-.805 5.96 5.96 0 0 1-2.183-2.183A5.863 5.863 0 0 1 2 8c0-1.09.268-2.092.805-3.012a5.96 5.96 0 0 1 2.183-2.183A5.863 5.863 0 0 1 8 2c1.09 0 2.092.268 3.012.805a5.96 5.96 0 0 1 2.183 2.183C13.732 5.908 14 6.91 14 8z"
          style={{ fill: theme.colors.baseFontColor }}
        />
      )}
    </ThemedIcon>
  );
}

function ClosedStatusIcon(iconProps: IconProps) {
  return (
    <ThemedIcon {...iconProps}>
      {({ theme }) => (
        <path
          d="M14 8c0 1.09-.268 2.092-.805 3.012a5.96 5.96 0 0 1-2.183 2.183A5.863 5.863 0 0 1 8 14a5.863 5.863 0 0 1-3.012-.805 5.96 5.96 0 0 1-2.183-2.183A5.863 5.863 0 0 1 2 8c0-1.09.268-2.092.805-3.012a5.96 5.96 0 0 1 2.183-2.183A5.863 5.863 0 0 1 8 2c1.09 0 2.092.268 3.012.805a5.96 5.96 0 0 1 2.183 2.183C13.732 5.908 14 6.91 14 8z"
          style={{ fill: theme.colors.baseFontColor }}
        />
      )}
    </ThemedIcon>
  );
}
