/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
const baseFontSizeRaw = 13;

module.exports = {
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
    red: '#D02F3A',
    purple: '#9139d4',

    gray94: '#efefef',
    gray80: '#cdcdcd',
    gray71: '#b4b4b4',
    gray67: '#aaa',
    gray60: '#888',
    gray52: '#525252',
    gray40: '#404040',

    disabledQualityGate: '#757575',

    sizeRatingBackground: '#297BAE',

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

    educationPrinciplesBgColor: '#F4F6FF',
    educationPrinciplesBorderColor: '#B0BDF9',

    favoriteColor: '#e77213',
    homepageColor: '#D86C13',

    // table
    rowHoverHighlight: '#ecf6fe',

    // fonts
    baseFontColor: '#333',
    secondFontColor: '#656565',

    // forms
    mandatoryFieldColor: '#a4030f',

    // leak
    leakPrimaryColor: '#fbf3d5',
    leakSecondaryColor: '#f1e8cb',

    // issues
    secondIssueBgColor: '#f8eeee',
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

    // badge
    badgeBlueBackground: '#2E7CB5',
    badgeBlueColor: '#FFFFFF',
    badgeRedBackgroundOnIssue: '#EEC8C8',

    // alm
    azure: '#0078d7',
    bitbucket: '#0052CC',
    github: '#e1e4e8',

    // code/pre
    codeBackground: '#f5f5f5',
    codeBorder: '#e6e6e6',
    codeAdded: '#dff0d8',
    codeRemoved: '#f2dede',

    // promotion
    darkBackground: '#292929',
    darkBackgroundSeparator: '#413b3b',
    darkBackgroundFontColor: '#f6f8fa',

    // new color palette
    // Some of these colors duplicate what we have above, but this will make it
    // easier to align with the UX designers on what colors to use where.
    // Colors that have transparency are suffixed with an "a" followed by the percentage
    // value of the alpha channel.
    primary: '#236a97',
    primarya40: 'rgba(35, 107, 151, 0.40)',
    primary400: '#297BAE',

    info50: '#ECF6FE',
    info500: '#0271B9',
    info400: '#4B9FD5',

    success500: '#008A25',
    success500a20: 'rgba(0, 138, 37, 0.20)',
    successVariant: '#B0D513',
    successVarianta20: 'rgba(177, 213, 19, 0.20)',
    successVariantDark: '#809E00',

    warning: '#B95E04',
    warningVariant: '#EABE06',
    warningVarianta20: 'rgba(234, 188, 6, 0.20)',
    warningVariantDark: '#B18F00',
    warningAccent: '#ED7D20',
    warningAccenta20: 'rgba(237, 124, 32, 0.20)',

    error500: '#D02F3A',
    error500a20: 'rgba(208, 47, 58, 0.20)',

    neutral200: '#CCCCCC',
    neutral600: '#666666',
    neutral800: '#333333',

    white: '#FFFFFF',

    black: '#000000',
    blacka87: 'rgba(0, 0, 0, 0.87)',
    blacka38: 'rgba(0, 0, 0, 0.38)'
  },

  sizes: {
    gridSize: `${grid}px`,

    baseFontSize: `${baseFontSizeRaw}px`,
    verySmallFontSize: '10px',
    smallFontSize: '12px',
    mediumFontSize: '14px',
    bigFontSize: '16px',
    hugeFontSize: '24px',
    giganticFontSize: '36px',

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
    baseFontSizeRaw,
    globalNavHeightRaw: 6 * grid,
    globalNavContentHeightRaw: 4 * grid,
    contextNavHeightRaw: 9 * grid
  },

  fonts: {
    baseFontFamily: "'Helvetica Neue', Helvetica, Arial, sans-serif",
    systemFontFamily: "-apple-system,'BlinkMacSystemFont','Helvetica','Arial',sans-serif",
    sourceCodeFontFamily: "Consolas, 'Ubuntu Mono', 'Liberation Mono', Menlo, Courier, monospace"
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
    pageSideZIndex: '50',
    pageHeaderZIndex: '55',

    globalBannerZIndex: '60',

    contextbarZIndex: '420',

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
