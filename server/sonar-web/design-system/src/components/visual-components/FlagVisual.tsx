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

interface Props {
  className?: string;
}

export function FlagVisual({ className }: Props) {
  const theme = useTheme();

  return (
    <svg
      className={className}
      fill="none"
      height="168"
      width="168"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M49.05 153.5c-10.87-1-15.29 5-15.97 8.5H16.44c1.7-6.5 14.26-19.5 32.61-8.5Z"
        fill={themeColor('illustrationSecondary')({ theme })}
      />
      <path
        clipRule="evenodd"
        d="M18.34 162h38.32c-2.83-6.1-9.95-11-19.16-11s-16.33 4.9-19.16 11Zm-7.12 3.01C12.92 153.72 24.03 145 37.5 145s24.59 8.72 26.28 20.01c.24 1.64-1.12 2.99-2.78 2.99H14c-1.66 0-3.02-1.35-2.78-2.99Z"
        fill="var(--echoes-color-icon-subdued)"
        fillRule="evenodd"
      />
      <path
        d="M46 98h87c-32.68-4.13-67.2-4.58-81.02-55H46v55ZM42 12c-9 5-6.5 11-4.5 16.5l-5-.5c-5.5-9.5 0-15 9.5-16Z"
        fill={themeColor('illustrationSecondary')({ theme })}
      />
      <path
        clipRule="evenodd"
        d="M37.5 27a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13Zm0 6a12.5 12.5 0 1 0 0-25 12.5 12.5 0 0 0 0 25Z"
        fill="var(--echoes-color-icon-subdued)"
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M40 33.41h-5V144.6h5V33.4ZM29 27v124h17V27H29Z"
        fill="var(--echoes-color-icon-subdued)"
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M40 37h41.11v6H46v55h89.42a1 1 0 0 0 .88-1.46l-11.84-22.81a7 7 0 0 1 0-6.46l11.84-22.8a1 1 0 0 0-.88-1.47h-6.3v-6h6.3a7 7 0 0 1 6.2 10.23l-11.83 22.8a1 1 0 0 0 0 .93l11.84 22.81a7 7 0 0 1-6.21 10.23H40V37Z"
        fill="var(--echoes-color-icon-subdued)"
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M117.18 5.7c-.96.74-2.03 1.35-3.18 1.8a13.33 13.33 0 0 1 7.5 7.5 13.29 13.29 0 0 1 7.5-7.5 13.33 13.33 0 0 1-7.5-7.5 13.29 13.29 0 0 1-4.32 5.7Zm2.55 1.8c.64.54 1.23 1.13 1.77 1.77.54-.64 1.13-1.23 1.77-1.77a16.35 16.35 0 0 1-1.77-1.77c-.54.64-1.13 1.23-1.77 1.77ZM87.92 30.06A45.26 45.26 0 0 1 81 33.5 45.26 45.26 0 0 1 106.5 59 45.21 45.21 0 0 1 132 33.5 45.24 45.24 0 0 1 106.5 8a45.21 45.21 0 0 1-18.58 22.06Zm5.71 3.44a51.28 51.28 0 0 1 12.87 12.87 51.28 51.28 0 0 1 12.87-12.87 51.28 51.28 0 0 1-12.87-12.87A51.28 51.28 0 0 1 93.63 33.5ZM132.01 17.38A26.64 26.64 0 0 1 127 20a26.64 26.64 0 0 1 15 15 26.62 26.62 0 0 1 15-15 26.64 26.64 0 0 1-15-15 26.61 26.61 0 0 1-9.99 12.38Zm4.07 2.62a31.13 31.13 0 0 1 5.92 5.92 31.13 31.13 0 0 1 5.92-5.92 31.13 31.13 0 0 1-5.92-5.92 31.13 31.13 0 0 1-5.92 5.92Z"
        fill={themeColor('illustrationPrimary')({ theme })}
        fillRule="evenodd"
      />
    </svg>
  );
}
