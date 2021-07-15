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
const grid = 8;

export default {
  colors: {
    blue: '#4b9fd5',
    veryLightBlue: '#f2faff',
    lightBlue: '#cae3f2',
    darkBlue: '#236a97',
    veryDarkBlue: '#0E516F',
    green: '#00aa00',
    lightGreen: '#b0d513',
    veryLightGreen: '#f5f9fc',
    yellow: '#eabe06',
    orange: '#ed7d20',
    red: '#d4333f',
    purple: '#9139d4',
    white: '#ffffff',

    gray94: '#efefef',
    gray80: '#cdcdcd',
    gray71: '#b4b4b4',
    gray67: '#aaa',
    gray60: '#999',
    gray40: '#404040',

    transparentWhite: 'rgba(255,255,255,0.62)',
    transparentGray: 'rgba(200, 200, 200, 0.5)',
    transparentBlack: 'rgba(0, 0, 0, 0.25)',

    disableGrayText: '#bbb',
    disableGrayBorder: '#ddd',
    disableGrayBg: '#ebebeb',

    barBackgroundColor: '#f3f3f3',
    barBackgroundColorHighlight: '#f8f8f8',
    barBorderColor: '#e6e6e6',

    globalNavBarBg: '#262626',

    // fonts
    baseFontColor: '#444',
    secondFontColor: '#777',

    // forms
    mandatoryFieldColor: '#a4030f',

    // leak
    leakPrimaryColor: '#fbf3d5',
    leakSecondaryColor: '#f1e8cb',

    // issues
    issueBgColor: '#f2dede',
    hotspotBgColor: '#eeeff4',
    issueLocationSelected: '#f4b1b0',
    issueLocationHighlighted: '#e1e1f2',
    conciseIssueRed: '#d18582',
    conciseIssueRedSelected: '#a4030f',

    // coverage
    lineCoverageRed: '#a4030f',
    lineCoverageGreen: '#b4dd78',

    // alerts
    warningIconColor: '#eabe06',

    alertBorderError: '#f4b1b0',
    alertBackgroundError: '#f2dede',
    alertTextError: '#862422',
    alertIconError: '#a4030f',

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
    pagePadding: '20px',
  },

  rawSizes: {
    grid,
    globalNavHeightRaw: 6 * grid,
    globalNavContentHeightRaw: 4 * grid,
    contextNavHeightRaw: 9 * grid,
  },

  fonts: {
    baseFontFamily: "'Helvetica Neue', 'Segoe UI', Helvetica, Arial, sans-serif",
    systemFontFamily:
      "-apple-system,'BlinkMacSystemFont','Segoe UI','Helvetica','Arial',sans-serif",
    sonarcloudFontFamily:
      "Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif",
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

    // page elements
    pageMainZIndex: '50',

    // generic page fixed elements
    contextbarZIndex: '420',

    // generic ui elements
    popupZIndex: '5000',

    modalZIndex: '6001',
    modalOverlayZIndex: '6000',

    processContainerZIndex: '7000',

    dropdownMenuZIndex: '7500',

    tooltipZIndex: '8000',
  },

  others: {
    defaultShadow: '0 6px 12px rgba(0, 0, 0, 0.175)',
  },
};
