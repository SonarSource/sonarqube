/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

// IMPORTANT: any change in this file requires restart of the dev server
const grid = 8;

module.exports = {
  colors: {
    blue: '#4b9fd5',
    veryLightBlue: '#f2faff',
    lightBlue: '#cae3f2',
    darkBlue: '#236a97',
    veryDarkBlue: '#0E516F',
    green: '#00aa00',
    lineCoverageGreen: '#b4dd78',
    lightGreen: '#b0d513',
    veryLightGreen: '#f5f9fc',
    yellow: '#eabe06',
    orange: '#ed7d20',
    red: '#d4333f',
    lineCoverageRed: '#a4030f',
    purple: '#9139d4',

    conciseIssueRed: '#d18582',

    gray94: '#efefef',
    gray80: '#cdcdcd',
    gray71: '#b4b4b4',
    gray67: '#aaa',
    gray60: '#999',
    gray40: '#404040',

    transparentWhite: 'rgba(255,255,255,0.62)',
    transparentGray: 'rgba(200, 200, 200, 0.5)',

    disableGrayText: '#bbb',
    disableGrayBorder: '#ddd',
    disableGrayBg: '#ebebeb',

    barBackgroundColor: '#f3f3f3',
    barBorderColor: '#e6e6e6',

    baseFontColor: '#444',
    secondFontColor: '#777',

    leakColor: '#fbf3d5',
    leakColorHover: '#f0e7c4',
    leakBorderColor: '#eae3c7',

    globalNavBarBg: '#262626',

    snippetFontColor: '#f0f0f0',

    //issues
    issueBgColor: '#ffeaea',
    hotspotBgColor: '#eeeff4',

    // alerts
    warningIconColor: '#e2bf41',

    alertBorderError: '#ebccd1',
    alertBackgroundError: '#f2dede',
    alertTextError: '#862422',
    alertIconError: '#b81723',

    alertBorderWarning: '#faebcc',
    alertBackgroundWarning: '#fcf8e3',
    alertTextWarning: '#6f4f17',
    alertIconWarning: '#db781a',

    alertBorderSuccess: '#d6e9c6',
    alertBackgroundSuccess: '#dff0d8',
    alertTextSuccess: '#215821',
    alertIconSuccess: '#6d9867',

    alertBorderInfo: '#b1dff3',
    alertBackgroundInfo: '#d9edf7',
    alertTextInfo: '#0e516f',
    alertIconInfo: '#0271b9',

    // sonarcloud
    sonarcloudOrange500: '#fd6a00',
    sonarcloudOrange600: '#e26003',
    sonarcloudOrange700: '#db5700',

    sonarcloudBlack100: '#ffffff',
    sonarcloudBlack200: '#f9f9fb',
    sonarcloudBlack250: '#e6e8ea',
    sonarcloudBlack300: '#cfd3d7',
    sonarcloudBlack500: '#8a8c8f',
    sonarcloudBlack700: '#434447',
    sonarcloudBlack800: '#2d3032',
    sonarcloudBlack900: '#070706',

    sonarcloudBlue500: '#4c9bd6',
    sonarcloudBlue600: '#327bb3',
    sonarcloudBlue900: '#0b3c62',

    sonarcloudBorderGray: 'rgba(207, 211, 215, 0.5)'
  },

  sizes: {
    gridSize: `${grid}px`,

    baseFontSize: '13px',
    verySmallFontSize: '10px',
    smallFontSize: '12px',
    mediumFontSize: '14px',
    bigFontSize: '16px',
    hugeFontSize: '24px',

    hugeControlHeight: `${5 * grid}px`,
    largeControlHeight: `${4 * grid}px`,
    controlHeight: `${3 * grid}px`,
    smallControlHeight: `${2.5 * grid}px`,
    tinyControlHeight: `${2 * grid}px`,

    globalNavHeight: `${6 * grid}px`,

    globalNavContentHeight: `${4 * grid}px`,

    maxPageWidth: '1320px',
    minPageWidth: '1080px',
    pagePadding: '20px'
  },

  rawSizes: {
    grid,
    globalNavHeightRaw: 6 * grid,
    globalNavContentHeightRaw: 4 * grid,
    contextNavHeightRaw: 9 * grid
  },

  fonts: {
    baseFontFamily: "'Helvetica Neue', 'Segoe UI', Helvetica, Arial, sans-serif",
    systemFontFamily:
      "-apple-system,'BlinkMacSystemFont','Segoe UI','Helvetica','Arial',sans-serif",
    sonarcloudFontFamily:
      "Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif"
  },

  // z-index
  // =======
  //    1 -  100  for page elements (e.g. sidebars, panels)
  //  101 -  500  for generic page fixed elements (e.g. navigation, workspace)
  //  501 - 3000  for page ui elements
  // 3001 - 8000  for generic ui elements (e.g. dropdowns, tooltips)
  zIndexes: {
    // common
    aboveNormalZIndex: '3',
    normalZIndex: '2',
    belowNormalZIndex: '1',

    // ui elements
    pageMainZIndex: '50',

    tooltipZIndex: '8000',

    dropdownMenuZIndex: '7500',

    processContainerZIndex: '7000',

    modalZIndex: '6001',
    modalOverlayZIndex: '6000',

    popupZIndex: '5000'
  },

  others: {
    defaultShadow: '0 6px 12px rgba(0, 0, 0, 0.175)'
  }
};
