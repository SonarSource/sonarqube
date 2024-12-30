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

import { keyframes, useTheme } from '@emotion/react';
import styled from '@emotion/styled';
import { themeColor } from '../../helpers/theme';

export function InProgressVisual() {
  const theme = useTheme();

  return (
    <svg className="svg-animated" height="168" width="168" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M149 151.15v-61.5c-6 48.4-49.17 61.34-70 61.5h70Z"
        fill={themeColor('illustrationShade')({ theme })}
      />
      <path
        d="M50.94 16.79 34 9.79 37.8 4l13.14 12.79ZM48.5 24.46 38 27.93V21l10.5 3.46ZM125.55 37.07l3.63-9.07 5.1 4.7-8.73 4.37ZM125 43.46 141.5 40v6.93L125 43.46ZM56.93 10.59 50 2.57 56.51 0l.42 10.59Z"
        fill={themeColor('illustrationPrimary')({ theme })}
      />
      <path
        d="M19 57.15v95h8v-95h-8ZM33 73.15h15v-8H33v8ZM56 73.15h15v-8H56v8Z"
        fill={themeColor('illustrationSecondary')({ theme })}
      />
      <path
        clipRule="evenodd"
        d="M20 157a7 7 0 0 1-7-7V61a7 7 0 0 1 7-7h28.5v6H20a1 1 0 0 0-1 1v16.88h63v6.24H19V150a1 1 0 0 0 1 1h128a1 1 0 0 0 1-1V61a1 1 0 0 0-1-1h-11v-6h11a7 7 0 0 1 7 7v89a7 7 0 0 1-7 7H20Z"
        fill="var(--echoes-color-icon-subdued)"
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M91 112.15H66v-6h25v6ZM62.09 129.5 48.6 142.54l-8.72-8.61 4.22-4.27 4.55 4.49 9.25-8.97 4.18 4.32ZM62.09 105.31 48.6 118.35l-8.72-8.6 4.22-4.28 4.55 4.5L57.9 101l4.18 4.31ZM91 137.34H66v-6h25v6Z"
        fill={themeColor('illustrationSecondary')({ theme })}
        fillRule="evenodd"
      />
      <Wheel>
        <path
          clipRule="evenodd"
          d="m115.17 46.11-7.2-4.15a24.21 24.21 0 0 0 1.72-6.41H118v-6.1h-8.31c-.28-2.24-.87-4.4-1.72-6.4l7.2-4.16-3.05-5.28-7.2 4.16a24.55 24.55 0 0 0-4.69-4.7l4.16-7.2-5.28-3.04-4.15 7.2a24.21 24.21 0 0 0-6.41-1.72V0h-6.1v8.31c-2.24.28-4.4.87-6.4 1.72l-4.16-7.2-5.28 3.05 4.16 7.2a24.52 24.52 0 0 0-4.7 4.69l-7.2-4.16-3.04 5.28 7.2 4.15a24.2 24.2 0 0 0-1.72 6.41H53v6.1h8.31c.28 2.24.87 4.4 1.72 6.4l-7.2 4.16 3.05 5.28 7.2-4.16a24.52 24.52 0 0 0 4.69 4.7l-4.16 7.2 5.28 3.04 4.15-7.2c2.02.85 4.17 1.44 6.41 1.72V65h6.1v-8.31a24.2 24.2 0 0 0 6.4-1.72l4.16 7.2 5.28-3.05-4.16-7.2a24.51 24.51 0 0 0 4.7-4.69l7.2 4.16 3.04-5.28ZM85.5 51a18.5 18.5 0 1 0 0-37 18.5 18.5 0 0 0 0 37Z"
          fill={themeColor('illustrationPrimary')({ theme })}
          fillRule="evenodd"
        />
      </Wheel>
      <path
        clipRule="evenodd"
        d="M73 32.5a12.5 12.5 0 0 0 25 0h6a18.5 18.5 0 1 1-37 0h6Z"
        fill={themeColor('illustrationInlineBorder')({ theme })}
        fillRule="evenodd"
      />
      <WheelInverted>
        <path
          clipRule="evenodd"
          d="m105.3 54.74 4.74-2.74 1.93 3.34a18.95 18.95 0 0 1 14.2.06l1.97-3.4 4.74 2.74-1.98 3.44A18.98 18.98 0 0 1 137.76 70H142v6h-4.24a18.98 18.98 0 0 1-6.98 11.91l2.1 3.65-4.74 2.74-2.1-3.64a18.95 18.95 0 0 1-13.93.05l-2.07 3.6-4.74-2.75 2.05-3.55A18.98 18.98 0 0 1 100.24 76H96v-6h4.24a18.98 18.98 0 0 1 6.99-11.91l-1.93-3.35ZM119 86a13 13 0 1 0 0-26 13 13 0 0 0 0 26Z"
          fill={themeColor('illustrationSecondary')({ theme })}
          fillRule="evenodd"
        />
      </WheelInverted>
      <circle cx="119" cy="73" fill={themeColor('illustrationPrimary')({ theme })} r="5" />
    </svg>
  );
}

const rotateKeyFrame = keyframes`
  from {
    transform: rotateZ(0deg);
  }
  to {
    transform: rotateZ(360deg);
  }
`;

const rotateKeyFrameInverse = keyframes`
  from {
    transform: rotateZ(360deg);
  }
  to {
    transform: rotateZ(0deg);
  }
`;

const Wheel = styled.g`
  transform-origin: 85.5px 32.5px 0;
  animation: ${rotateKeyFrame} 3s infinite;
`;

const WheelInverted = styled.g`
  transform-origin: 119px 73px 0;
  animation: ${rotateKeyFrameInverse} 3s infinite;
`;
