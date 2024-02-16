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

import { Global, css, useTheme } from '@emotion/react';
import { themeColor } from 'design-system';
import React from 'react';
import twDefaultTheme from 'tailwindcss/defaultTheme';

export function GlobalStyles() {
  const theme = useTheme();

  return (
    <Global
      styles={css`
        body {
          font-family: Inter, ${twDefaultTheme.fontFamily.sans.join(', ')};
          font-size: 0.875rem;
          line-height: 1.25rem;
          font-weight: 400;

          color: ${themeColor('pageContent')({ theme })};
          background-color: ${themeColor('backgroundPrimary')({ theme })};
        }

        a {
          outline: none;
          text-decoration: none;
          color: ${themeColor('pageContent')({ theme })};
        }

        ol,
        ul {
          padding-left: 0;
          list-style: none;
        }
      `}
    />
  );
}
