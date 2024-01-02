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
const plugin = require('tailwindcss/plugin');

module.exports = plugin(({ addUtilities, theme }) => {
  const newUtilities = {
    '.heading-xl': {
      'font-family': theme('fontFamily.sans'),
      'font-size': theme('fontSize.xl'),
      'line-height': theme('fontSize').xl[1],
      'font-weight': theme('fontWeight.semibold'),
    },
    '.heading-lg': {
      'font-family': theme('fontFamily.sans'),
      'font-size': theme('fontSize.lg'),
      'line-height': theme('fontSize').lg[1],
      'font-weight': theme('fontWeight.semibold'),
    },
    '.heading-md': {
      'font-family': theme('fontFamily.sans'),
      'font-size': theme('fontSize.md'),
      'line-height': theme('fontSize').md[1],
      'font-weight': theme('fontWeight.semibold'),
    },
    '.body-md': {
      'font-family': theme('fontFamily.sans'),
      'font-size': theme('fontSize.base'),
      'line-height': theme('fontSize').base[1],
      'font-weight': theme('fontWeight.regular'),
    },
    '.body-md-highlight': {
      'font-family': theme('fontFamily.sans'),
      'font-size': theme('fontSize.base'),
      'line-height': theme('fontSize').base[1],
      'font-weight': theme('fontWeight.semibold'),
    },
    '.body-sm': {
      'font-family': theme('fontFamily.sans'),
      'font-size': theme('fontSize.sm'),
      'line-height': theme('fontSize').sm[1],
      'font-weight': theme('fontWeight.regular'),
    },
    '.body-xs': {
      'font-family': theme('fontFamily.sans'),
      'font-size': theme('fontSize.xs'),
      'line-height': theme('fontSize').xs[1],
      'font-weight': theme('fontWeight.regular'),
    },
    '.body-sm-highlight': {
      'font-family': theme('fontFamily.sans'),
      'font-size': theme('fontSize.sm'),
      'line-height': theme('fontSize').sm[1],
      'font-weight': theme('fontWeight.semibold'),
    },
    '.code': {
      'font-family': theme('fontFamily.mono'),
      'font-size': theme('fontSize.sm'),
      'line-height': theme('fontSize').code[1],
      'font-weight': theme('fontWeight.regular'),
    },
    '.code-highlight': {
      'font-family': theme('fontFamily.mono'),
      'font-size': theme('fontSize.sm'),
      'line-height': theme('fontSize').code[1],
      'font-weight': theme('fontWeight.bold'),
    },
    '.code-comment': {
      'font-family': theme('fontFamily.mono'),
      'font-size': theme('fontSize.sm'),
      'line-height': theme('fontSize').code[1],
      'font-style': 'italic',
    },
  };

  addUtilities(newUtilities);
});
