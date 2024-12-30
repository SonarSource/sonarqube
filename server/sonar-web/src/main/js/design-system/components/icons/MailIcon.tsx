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
import { themeColor } from '~design-system';

export function MailIcon() {
  const theme = useTheme();

  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
      <g id="mail">
        <mask
          id="mask0_6548_2965"
          style={{ maskType: 'alpha' }}
          maskUnits="userSpaceOnUse"
          x="0"
          y="0"
          width="16"
          height="16"
        >
          <rect id="Bounding box" width="16" height="16" fill="#D9D9D9" />
        </mask>
        <g mask="url(#mask0_6548_2965)">
          <path
            id="mail_2"
            d="M2.66659 13.3332C2.29992 13.3332 1.98603 13.2026 1.72492 12.9415C1.46381 12.6804 1.33325 12.3665 1.33325 11.9998V3.99984C1.33325 3.63317 1.46381 3.31928 1.72492 3.05817C1.98603 2.79706 2.29992 2.6665 2.66659 2.6665H13.3333C13.6999 2.6665 14.0138 2.79706 14.2749 3.05817C14.536 3.31928 14.6666 3.63317 14.6666 3.99984V11.9998C14.6666 12.3665 14.536 12.6804 14.2749 12.9415C14.0138 13.2026 13.6999 13.3332 13.3333 13.3332H2.66659ZM7.99992 8.6665L2.66659 5.33317V11.9998H13.3333V5.33317L7.99992 8.6665ZM7.99992 7.33317L13.3333 3.99984H2.66659L7.99992 7.33317ZM2.66659 5.33317V3.99984V11.9998V5.33317Z"
            fill={themeColor('currentColor')({ theme })}
          />
        </g>
      </g>
    </svg>
  );
}
