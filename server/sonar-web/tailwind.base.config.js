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
const path = require('path');
const { fontFamily } = require('tailwindcss/defaultTheme');
const utilities = require('./tailwind-utilities');

module.exports = {
  prefix: 'sw-', // Prefix all tailwind classes with the sw- prefix to avoid collisions
  theme: {
    // Define cursors
    cursor: {
      auto: 'auto',
      default: 'default',
      pointer: 'pointer',
      text: 'text',
      'not-allowed': 'not-allowed',
    },
    // Define font sizes
    fontSize: {
      code: ['0.875rem', '1.125rem'], // 14px / 18px
      xs: ['0.75rem', '1rem'], // 12px / 16px
      sm: ['0.875rem', '1.25rem'], // 14px / 20px
      base: ['1rem', '1.5rem'], // 16px / 24px
      md: ['1.313rem', '1.75rem'], // 21px / 28px
      lg: ['1.5rem', '1.75rem'], // 24px / 28px
      xl: ['2.25rem', '3rem'], // 36px / 48px
    },
    // Define font weights
    fontWeight: {
      regular: 400,
      semibold: 600,
      bold: 700,
    },
    // Define font families
    fontFamily: {
      sans: ['Inter', ...fontFamily.sans],
      mono: ['Ubuntu Mono', ...fontFamily.mono],
    },
    // Define less order properties than default
    order: {
      first: '-9999',
      last: '9999',
      none: '0',
      1: '1',
      2: '2',
      3: '3',
      4: '4',
    },
    screens: {
      sm: '1280px',
      lg: '1920px',
    },
    // Defined spacing values based on our grid size
    spacing: {
      0: '0',
      '1/2': '0.125rem', // 2px
      1: '0.25rem', // 4px
      2: '0.5rem', // 8px
      3: '0.75rem', // 12px
      4: '1rem', // 16px
      5: '1.25rem', // 20px
      6: '1.5rem', // 24px
      7: '1.75rem', // 28px
      8: '2rem', // 32px
      9: '2.25rem', // 36px
      10: '2.5rem', // 40px
      12: '3rem', // 48px
      14: '3.75rem', // 60px
      16: '4rem', // 64px
      24: '6rem', // 96px
      32: '8rem', // 128px
      40: '10rem', // 160px
      64: '16rem', // 256px

      page: '1.25rem', // 20px
    },
    maxHeight: (twTheme) => twTheme('height'),
    maxWidth: (twTheme) => twTheme('width'),
    minHeight: (twTheme) => twTheme('height'),
    minWidth: (twTheme) => twTheme('width'),
    borderRadius: {
      0: '0',
      '1/2': '0.125rem', // 2px
      1: '0.25rem', // 4px
      2: '0.5rem', // 8px
      pill: '625rem',
    },
    zIndex: {
      normal: '2',
      'issue-header': '10',
      'project-list-header': '30',
      filterbar: '50',
      'content-popup': '52',
      'filterbar-header': '55',
      'dropdown-menu-page': '100',
      'top-navbar': '419',
      popup: '420',
      'global-navbar': '421',
      sidebar: '421',
      'core-concepts': '422',
      'global-popup': '5000',
      'dropdown-menu': '7500',
      'modal-overlay': 8500,
      modal: '9000',
      tooltip: '9001',
    },
    extend: {
      width: {
        'abs-150': '150px',
        'abs-200': '200px',
        'abs-250': '250px',
        'abs-300': '300px',
        'abs-350': '350px',
        'abs-400': '400px',
        'abs-500': '500px',
        'abs-600': '600px',
        'abs-800': '800px',
        'input-small': '150px',
        'input-medium': '250px',
        'input-large': '350px',
        icon: '1rem', // 16px
      },
      height: {
        'abs-200': '200px',
        icon: '1rem', // 16px
        control: '2.25rem', // 36px
      },
    },
  },
  corePlugins: {
    // Please respect the alphabetical order in the below plugins
    alignItems: true, // .sw-items-x classes
    alignSelf: true, // .sw-self-x classes
    borderRadius: true, // .sw-rounded-x classes
    boxSizing: true, // .sw-box-x classes
    cursor: true, // .sw-cursor-not-allowed
    display: true, // display classes .sw-grid .sw-flex
    flex: true, // .sw-flex-1 .sw-flex-auto ... classes
    flexDirection: true, // .sw-flex-row .sw-flex-col-reverse ... classes
    flexGrow: true, // .sw-flex-grow .sw-flex-grow-0 classes
    flexShrink: true, // .sw-flex-shrink .sw-flex-shrink-0 classes
    flexWrap: true, // .sw-flex-wrap sw-flex-nowrap ... classes
    fontFamily: true, // .sw-font-sans .sw-font-mono classes
    fontSize: true, // .sw-text-sm and similar classes
    fontWeight: true, // .sw-font-x classes
    gap: true, // .sw-gap-x classes based on spacing
    gridAutoFlow: true, // all css grid related classes: .sw-grid-cols-x .sw-col-span-x
    gridColumn: true,
    gridColumnEnd: true,
    gridColumnStart: true,
    gridRow: true,
    gridRowEnd: true,
    gridRowStart: true,
    gridTemplateColumns: true,
    gridTemplateRows: true,
    height: true, // height classes .sw-h-x based on spacing + some more
    inset: true, // placement classes .sw-top-x based on spacing + some more
    justifyContent: true, // .sw-justify-x classes
    lineHeight: true, // .sw-leading-x classes
    margin: true, // .sw-m-x classes based on spacing
    maxHeight: true, // sw-max-height classes .sw-max-h-x based on spacing + some more
    maxWidth: true, // sw-max-width classes .sw-max-w-x based on spacing + some more
    minHeight: true, // sw-min-height classes .sw-min-h-x based on spacing + some more
    minWidth: true, // sw-min-width classes .sw-min-w-x based on spacing + some more
    opacity: true, // sw-opacity-x classes
    order: true, // .sw-order-x classes
    overflow: true, // .sw-overflow-x classes
    padding: true, // .sw-p-x classes based on spacing
    pointerEvents: true, //.sw-pointer-events-none .sw-pointer-events-auto
    position: true, // position classes .sw-relative .sw-absolute
    preflight: false, // disable preflight
    textAlign: true, // .sw-text-x classes
    textOverflow: true, // .sw-text-ellipsis, .sw-truncate
    textTransform: true, // sw-uppercase, .sw-capitalize
    userSelect: true, // .sw-select-none classes
    verticalAlign: true, // .sw-align-x classes
    width: true, // .sw-w-x classes based on spacing + some more
    whitespace: true, // sw-whitespace-x classes
    wordBreak: true, // .sw-break-normal, sw-break-all, sw-break-words classes
    zIndex: true, // .sw-z-x classes
  },
  plugins: [utilities],
  content: [
    path.resolve(__dirname, './src/**/!(__tests__|@types|api)/*.{ts,tsx}'),
    path.resolve(__dirname, './design-system/src/**/!(__tests__|@types|theme|helpers)/*.{ts,tsx}'),
  ],
};
