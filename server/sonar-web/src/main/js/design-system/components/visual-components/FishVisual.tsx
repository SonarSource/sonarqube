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

export function FishVisual({ className }: Props) {
  const theme = useTheme();

  return (
    <svg
      className={className}
      fill="none"
      height="168"
      width="168"
      xmlns="http://www.w3.org/2000/svg"
    >
      <g clipPath="url(#a)">
        <path
          d="M7 96.939c44.414-18.728 131.812 14.152 153-.439-.653 2.194-2.176 6.256-3.482 9.214C126.569 125.531 82.424 95.788 17.229 118 9.612 105.934 7 96.939 7 96.939ZM163.931 77.355C115.704 96.06 27.656 67.785 4.649 82.357 4.216 79.85 4 75.245 4 71.765 36.52 51.972 92.273 80.185 163.065 58c1.299 15.294.866 19.355.866 19.355ZM161 51.205C146.124 49.19 78.726 42.909 5 56a122.739 122.739 0 0 1 2.813-10.99c28.002-12.889 125.425-4.826 151.24-.798A745.03 745.03 0 0 1 161 51.205Z"
          fill={themeColor('illustrationShade')({ theme })}
        />
        <path
          d="M134 133.844C120.137 144.595 102.699 151 83.758 151 57.4 151 33.956 138.598 19 119.341c61.278-7.139 76.068 16.657 115 14.503Z"
          fill={themeColor('illustrationSecondary')({ theme })}
        />
        <path
          clipRule="evenodd"
          d="M26.371 19.118C13.431 33.093 5.526 51.785 5.526 72.328c0 25.124 11.826 47.485 30.228 61.828 13.287 10.356 29.995 16.525 48.152 16.525 43.288 0 78.379-35.08 78.379-78.353 0-20.543-7.905-39.235-20.845-53.21l3.691-3.416c13.767 14.868 22.184 34.767 22.184 56.626 0 46.051-37.344 83.382-83.41 83.382-19.315 0-37.104-6.567-51.244-17.588C13.091 122.868.496 99.068.496 72.328c0-21.86 8.418-41.758 22.184-56.626l3.691 3.416Z"
          fill="var(--echoes-color-icon-subdued)"
          fillRule="evenodd"
        />
        <path
          clipRule="evenodd"
          d="m27.073 19.091-.338.364C13.877 33.343 6.023 51.916 6.023 72.328c0 24.964 11.75 47.184 30.036 61.436 13.203 10.291 29.805 16.42 47.847 16.42 43.014 0 77.883-34.858 77.883-77.856 0-20.413-7.855-38.986-20.713-52.873l-.337-.364L145.158 15l.337.364c13.849 14.956 22.317 34.975 22.317 56.964 0 46.325-37.567 83.879-83.906 83.879-19.43 0-37.326-6.607-51.55-17.693C12.669 123.17 0 99.227 0 72.328c0-21.99 8.468-42.008 22.316-56.964l.337-.364 4.42 4.091Zm-.702.027-.013.015C13.426 33.105 5.526 51.791 5.526 72.328c0 25.124 11.826 47.485 30.228 61.828 13.287 10.356 29.995 16.525 48.152 16.525 43.288 0 78.379-35.08 78.379-78.353 0-20.536-7.9-39.223-20.832-53.196a.18.18 0 0 0-.013-.014l3.691-3.416.014.015.322.35c13.568 14.828 21.848 34.58 21.848 56.26 0 46.052-37.344 83.383-83.41 83.383-19.315 0-37.104-6.567-51.244-17.588C13.091 122.868.496 99.068.496 72.328c0-21.68 8.28-41.433 21.848-56.261l.32-.347.016-.018 3.691 3.416Z"
          fill="var(--echoes-color-icon-subdued)"
          fillRule="evenodd"
        />
        <path
          d="M68 19a5 5 0 1 0 10 0 5 5 0 0 0-10 0ZM76 40.925a5 5 0 1 0 10 0 5 5 0 0 0-10 0ZM68 62.12a5 5 0 1 0 10 0 5 5 0 0 0-10 0Z"
          fill={themeColor('illustrationSecondary')({ theme })}
        />
        <path
          d="M152.238 54.288c-2.47.206-5.832 4.717-7.72 6.69l1.544 9.264c1.544 1.287 3.603 5.661 7.205 5.918 3.603.258 5.662-5.918 5.662-11.58 0-5.753-3.603-10.55-6.691-10.292Z"
          fill={themeColor('illustrationPrimary')({ theme })}
        />
        <path
          d="M146.062 70.242c10.035 34.224-51.723 17.498-63.303 6.176-.772-.258-1.08-3.448 1.802-2.83 9.263 1.028 10.035-7.206 10.55-11.066.515-3.86 3.345-17.755 25.218-21.1 21.873-3.346 27.019 14.152 23.931 19.814 4.375-.258 7.72 6.433 1.802 9.006Z"
          fill={themeColor('illustrationPrimary')({ theme })}
        />
        <path
          d="M100.49 70.868a5.844 5.844 0 1 0 11.689 0 5.844 5.844 0 0 0-11.689 0Z"
          fill={themeColor('illustrationPrimary')({ theme })}
        />
        <path
          clipRule="evenodd"
          d="M106.334 72.878a2.01 2.01 0 1 1 0-4.02 2.01 2.01 0 0 1 0 4.02Zm0 3.834a5.844 5.844 0 1 1 0-11.688 5.844 5.844 0 0 1 0 11.688Z"
          fill={themeColor('backgroundSecondary')({ theme })}
          fillRule="evenodd"
        />
        <path
          clipRule="evenodd"
          d="M115.047 107.823a1.78 1.78 0 1 1-3.301-1.332 1.78 1.78 0 0 1 3.301 1.332Zm-1.879 4.479a5.15 5.15 0 0 0 2.154-9.918 5.15 5.15 0 0 0-5.285 8.676l-1.572 3.898-8.012-3.231-1.26 3.123 8.012 3.232-6.841 16.96c-6.675-3.451-9.804-11.129-8.727-13.8l-2.842-1.147c-2.348 5.824 2.488 12.284 6.155 17.183 1.94 2.591 3.552 4.745 3.601 6.137.995-.964 3.62-1.374 6.785-1.869 6.029-.943 14.017-2.191 16.422-8.155l-2.842-1.146c-1.125 2.79-8.357 5.916-15.41 4.013l6.822-16.916 7.526 3.035 1.26-3.124-7.526-3.035 1.58-3.916Zm8.945 16.385-5.849 2.487 8.336 3.363-2.487-5.85ZM91.8 116.461l2.487 5.849-8.336-3.362 5.85-2.487Z"
          fill="var(--echoes-color-icon-subdued)"
          fillRule="evenodd"
        />
      </g>
      <defs>
        <clipPath id="a">
          <path d="M0 0h168v168H0z" fill={themeColor('backgroundSecondary')({ theme })} />
        </clipPath>
      </defs>
    </svg>
  );
}
