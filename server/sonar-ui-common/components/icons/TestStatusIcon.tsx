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
  ok: OkTestStatusIcon,
  failure: FailureTestStatusIcon,
  error: ErrorTestStatusIcon,
  skipped: SkippedTestStatusIcon,
};

export default function TestStatusIcon({ status, ...iconProps }: Props) {
  const Icon = statusIcons[status.toLowerCase()];
  return Icon ? <Icon {...iconProps} /> : null;
}

function OkTestStatusIcon(iconProps: IconProps) {
  return (
    <ThemedIcon {...iconProps}>
      {({ theme }) => (
        <path
          d="M12.03 6.734a.49.49 0 0 0-.14-.36l-.71-.702a.48.48 0 0 0-.352-.15.474.474 0 0 0-.35.15l-3.19 3.18-1.765-1.766a.479.479 0 0 0-.35-.15.479.479 0 0 0-.353.15l-.71.703a.482.482 0 0 0-.14.358c0 .14.046.258.14.352l2.828 2.828c.098.1.216.15.35.15.142 0 .26-.05.36-.15l4.243-4.242a.475.475 0 0 0 .14-.352l-.001.001zM14 8c0 1.09-.268 2.092-.805 3.012a5.96 5.96 0 0 1-2.183 2.183A5.863 5.863 0 0 1 8 14a5.863 5.863 0 0 1-3.012-.805 5.96 5.96 0 0 1-2.183-2.183A5.863 5.863 0 0 1 2 8c0-1.09.268-2.092.805-3.012a5.96 5.96 0 0 1 2.183-2.183A5.863 5.863 0 0 1 8 2c1.09 0 2.092.268 3.012.805a5.96 5.96 0 0 1 2.183 2.183C13.732 5.908 14 6.91 14 8z"
          style={{ fill: theme.colors.green }}
        />
      )}
    </ThemedIcon>
  );
}

function FailureTestStatusIcon(iconProps: IconProps) {
  return (
    <ThemedIcon {...iconProps}>
      {({ theme }) => (
        <path
          d="M8 14c-3.311 0-6-2.689-6-6s2.689-6 6-6 6 2.689 6 6-2.689 6-6 6zM7 9h2V4H7v5zm0 3h2v-2H7v2z"
          style={{ fill: theme.colors.orange, fillRule: 'nonzero' }}
        />
      )}
    </ThemedIcon>
  );
}

function ErrorTestStatusIcon(iconProps: IconProps) {
  return (
    <ThemedIcon {...iconProps}>
      {({ theme }) => (
        <path
          d="M10.977 9.766a.497.497 0 0 0-.149-.352L9.414 8l1.414-1.414a.497.497 0 0 0 0-.711l-.703-.703a.497.497 0 0 0-.71 0L8 6.586 6.586 5.172a.497.497 0 0 0-.711 0l-.703.703a.497.497 0 0 0 0 .71L6.586 8 5.172 9.414a.497.497 0 0 0 0 .711l.703.703a.497.497 0 0 0 .71 0L8 9.414l1.414 1.414a.497.497 0 0 0 .711 0l.703-.703a.515.515 0 0 0 .149-.36zM14 8c0 3.313-2.688 6-6 6-3.313 0-6-2.688-6-6 0-3.313 2.688-6 6-6 3.313 0 6 2.688 6 6z"
          style={{ fill: theme.colors.red, fillRule: 'nonzero' }}
        />
      )}
    </ThemedIcon>
  );
}

function SkippedTestStatusIcon(iconProps: IconProps) {
  return (
    <ThemedIcon {...iconProps}>
      {({ theme }) => (
        <path
          d="M11.5 8.5v-1c0-.273-.227-.5-.5-.5H5c-.273 0-.5.227-.5.5v1c0 .273.227.5.5.5h6c.273 0 .5-.227.5-.5zM14 8c0 3.313-2.688 6-6 6-3.313 0-6-2.688-6-6 0-3.313 2.688-6 6-6 3.313 0 6 2.688 6 6z"
          style={{ fill: theme.colors.gray71, fillRule: 'nonzero' }}
        />
      )}
    </ThemedIcon>
  );
}
