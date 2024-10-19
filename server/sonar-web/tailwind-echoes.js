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

module.exports = plugin(({ addUtilities }) => {
  const echoes = {
    '.code': {
      font: 'var(--echoes-typography-code-default)',
    },
    '.code-highlight': {
      font: 'var(--echoes-typography-code-highlight)',
    },
    '.code-comment': {
      font: 'var(--echoes-typography-code-comment)',
    },
    '.heading-xs': {
      font: 'var(--echoes-typography-heading-xsmall)',
    },
    '.heading-sm': {
      font: 'var(--echoes-typography-heading-small)',
    },
    '.heading-md': {
      font: 'var(--echoes-typography-heading-medium)',
    },
    '.heading-lg': {
      font: 'var(--echoes-typography-heading-large)',
    },
    '.heading-xl': {
      font: 'var(--echoes-typography-heading-xlarge)',
    },
    '.typo-default': {
      font: 'var(--echoes-typography-text-default-regular)',
    },
    '.typo-semibold': {
      font: 'var(--echoes-typography-text-default-semi-bold)',
    },
    '.typo-bold': {
      font: 'var(--echoes-typography-text-default-bold)',
    },
    '.typo-sm': {
      font: 'var(--echoes-typography-text-small-medium)',
    },
    '.typo-sm-semibold': {
      font: 'var(--echoes-typography-text-small-semi-bold)',
    },
    '.typo-lg': {
      font: 'var(--echoes-typography-text-large-regular)',
    },
    '.typo-lg-semibold': {
      font: 'var(--echoes-typography-text-large-semi-bold)',
    },
    '.typo-label': {
      font: 'var(--echoes-typography-others-label)',
    },
    '.typo-helper-text': {
      font: 'var(--echoes-typography-others-helper-text)',
    },
    '.typo-display': {
      font: 'var(--echoes-typography-display-default)',
    },
  };

  addUtilities(echoes);
});
