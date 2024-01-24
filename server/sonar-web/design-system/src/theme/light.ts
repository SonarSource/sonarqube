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
import { OPACITY_20_PERCENT, OPACITY_75_PERCENT } from '../helpers/constants';
import COLORS from './colors';

const primary = {
  light: COLORS.indigo[400],
  default: COLORS.indigo[500],
  dark: COLORS.indigo[600],
};

const secondary = {
  light: COLORS.blueGrey[50],
  default: COLORS.blueGrey[200],
  dark: COLORS.blueGrey[400],
  darker: COLORS.blueGrey[500],
};

const danger = {
  lightest: COLORS.red[50],
  lighter: COLORS.red[300],
  light: COLORS.red[400],
  default: COLORS.red[600],
  dark: COLORS.red[700],
  darker: COLORS.red[800],
};

const codeSnippetLight = {
  annotations: [34, 84, 192],
  body: [51, 53, 60],
  constants: [126, 83, 5],
  comments: [109, 111, 119],
  keyword: [152, 29, 150],
  string: [32, 105, 31],
  'keyword-light': [28, 28, 163], // Not used currently in code snippet
  'preprocessing-directive': [47, 103, 48],
};

export const lightTheme = {
  id: 'light-theme',
  highlightTheme: 'atom-one-light.css',
  logo: 'sonarcloud-logo-black.svg',

  colors: {
    transparent: 'transparent',
    currentColor: 'currentColor',

    backgroundPrimary: COLORS.blueGrey[25],
    backgroundSecondary: COLORS.white,
    border: COLORS.grey[50],
    sonarcloud: COLORS.sonarcloud,

    // primary
    primaryLight: primary.light,
    primary: primary.default,
    primaryDark: primary.dark,

    // danger
    danger: danger.dark,

    // text
    textDisabled: COLORS.blueGrey[300],
    textSubdued: COLORS.blueGrey[400],
    textSuccess: COLORS.yellowGreen[700],

    //Project list card
    projectCardDisabled: COLORS.blueGrey[200],

    // buttons
    button: primary.default,
    buttonHover: primary.dark,
    buttonSecondary: COLORS.white,
    buttonSecondaryBorder: secondary.default,
    buttonSecondaryHover: secondary.light,
    buttonDisabled: secondary.light,
    buttonDisabledBorder: secondary.default,

    // danger buttons
    dangerButton: danger.default,
    dangerButtonHover: danger.dark,
    dangerButtonFocus: danger.default,
    dangerButtonSecondary: COLORS.white,
    dangerButtonSecondaryBorder: danger.lighter,
    dangerButtonSecondaryHover: danger.lightest,
    dangerButtonSecondaryFocus: danger.light,

    // third party button
    thirdPartyButton: COLORS.white,
    thirdPartyButtonBorder: secondary.default,
    thirdPartyButtonHover: secondary.light,

    // popup
    popup: COLORS.white,
    popupBorder: secondary.default,

    // Toasts
    toast: COLORS.white,
    toastText: secondary.darker,
    toastCloseIcon: secondary.dark,

    toastErrorBorder: danger.light,
    toastErrorIconBackground: danger.lightest,

    toastWarningBorder: COLORS.yellow[400],
    toastWarningIconBackground: COLORS.yellow[50],

    toastSuccessBorder: COLORS.yellowGreen[400],
    toastSuccessIconBackground: COLORS.yellowGreen[50],

    toastInfoBorder: COLORS.blue[400],
    toastInfoIconBackground: COLORS.blue[50],

    // spotlight
    spotlightPulseBackground: primary.default,
    spotlightBackgroundColor: COLORS.blueGrey[50],

    // modal
    modalContents: COLORS.white,
    modalOverlay: [...COLORS.blueGrey[900], OPACITY_75_PERCENT],

    // dropdown menu
    dropdownMenu: COLORS.white,
    dropdownMenuHover: secondary.light,
    dropdownMenuFocus: COLORS.indigo[50],
    dropdownMenuFocusBorder: primary.light,
    dropdownMenuDisabled: COLORS.white,
    dropdownMenuHeader: COLORS.white,
    dropdownMenuDanger: danger.default,
    dropdownMenuSubTitle: secondary.dark,

    // radio
    radio: primary.default,
    radioBorder: primary.default,
    radioHover: COLORS.indigo[50],
    radioFocus: COLORS.indigo[50],
    radioFocusBorder: COLORS.indigo[300],
    radioFocusOutline: [...COLORS.indigo[300], OPACITY_20_PERCENT],
    radioChecked: COLORS.indigo[50],
    radioDisabled: secondary.default,
    radioDisabledBackground: secondary.light,
    radioDisabledBorder: secondary.default,
    radioDisabledLabel: COLORS.blueGrey[300],

    // switch
    switch: secondary.default,
    switchDisabled: COLORS.blueGrey[100],
    switchActive: primary.default,
    switchHover: COLORS.blueGrey[300],
    switchHoverActive: primary.light,
    switchButton: COLORS.white,
    switchButtonDisabled: secondary.light,

    // sidebar
    // NOTE: these aren't used because the sidebar is exclusively dark. but for type purposes are listed here
    sidebarBackground: COLORS.blueGrey[700],
    sidebarItemActive: COLORS.blueGrey[800],
    sidebarBorder: COLORS.blueGrey[500],
    sidebarTextDisabled: COLORS.blueGrey[400],
    sidebarIcon: COLORS.blueGrey[400],
    sidebarActiveIcon: COLORS.blueGrey[200],

    //separator-circle
    separatorCircle: COLORS.blueGrey[200],
    separatorSlash: COLORS.blueGrey[300],

    // rule breakdown table
    breakdownBorder: COLORS.grey[100],
    breakdownHeaderBackground: COLORS.blueGrey[50],

    // flag message
    flagMessageBackground: COLORS.white,

    errorBorder: danger.light,
    errorBackground: danger.lightest,
    errorText: danger.dark,

    warningBorder: COLORS.yellow[400],
    warningBackground: COLORS.yellow[50],
    warningText: COLORS.yellow[900],

    successBorder: COLORS.green[400],
    successBackground: COLORS.green[50],
    successText: COLORS.green[900],

    infoBorder: COLORS.blue[400],
    infoBackground: COLORS.blue[50],
    infoText: COLORS.blue[900],

    // banner message
    bannerMessage: danger.lightest,
    bannerMessageIcon: danger.darker,

    // toggle buttons
    toggle: COLORS.white,
    toggleBorder: secondary.default,
    toggleHover: secondary.light,
    toggleFocus: [...secondary.default, OPACITY_20_PERCENT],

    // code snippet
    codeSnippetBackground: COLORS.blueGrey[25],
    codeSnippetBorder: COLORS.blueGrey[100],
    codeSnippetHighlight: secondary.default,
    codeSnippetBody: codeSnippetLight.body,
    codeSnippetAnnotations: codeSnippetLight.annotations,
    codeSnippetComments: codeSnippetLight.comments,
    codeSnippetConstants: codeSnippetLight.constants,
    codeSnippetKeyword: codeSnippetLight.keyword,
    codeSnippetString: codeSnippetLight.string,
    codeSnippetKeywordLight: codeSnippetLight['keyword-light'],
    codeSnippetPreprocessingDirective: codeSnippetLight['preprocessing-directive'],
    codeSnippetInline: COLORS.blueGrey[500],

    // code viewer
    codeLine: COLORS.white,
    codeLineBorder: COLORS.grey[100],
    codeLineIssueIndicator: COLORS.blueGrey[400], // Should be blueGrey[300], to be changed once code viewer is reworked
    codeLineLocationMarker: COLORS.red[200],
    codeLineLocationMarkerSelected: danger.lighter,
    codeLineLocationSelected: COLORS.blueGrey[100],
    codeLineCoveredUnderline: [...COLORS.green[500], 0.15],
    codeLineUncoveredUnderline: [...COLORS.red[500], 0.15],

    codeLineHover: secondary.light,
    codeLineHighlighted: COLORS.blueGrey[100],
    codeLineNewCodeUnderline: [...COLORS.indigo[300], 0.15],
    codeLineMeta: COLORS.blueGrey[300],
    codeLineMetaHover: secondary.dark,
    codeLineDuplication: secondary.default,
    codeLineCovered: COLORS.green[300],
    codeLineUncovered: danger.default,
    codeLinePartiallyCoveredA: danger.default,
    codeLinePartiallyCoveredB: COLORS.white,
    codeLineIssueSquiggle: danger.lighter,
    codeLineIssuePointerBorder: COLORS.white,
    codeLineLocationHighlighted: [...COLORS.blueGrey[200], 0.6],
    codeLineEllipsis: COLORS.white,
    codeLineEllipsisHover: secondary.light,
    codeLineIssueLocation: [...danger.lighter, 0.15],
    codeLineIssueLocationSelected: [...danger.lighter, 0.5],
    codeLineIssueMessageTooltip: secondary.darker,

    // code syntax highlight
    codeSyntaxBody: COLORS.codeSyntaxLight.body,
    codeSyntaxAnnotations: COLORS.codeSyntaxLight.annotations,
    codeSyntaxConstants: COLORS.codeSyntaxLight.constants,
    codeSyntaxComments: COLORS.codeSyntaxLight.comments,
    codeSyntaxKeyword: COLORS.codeSyntaxLight.keyword,
    codeSyntaxString: COLORS.codeSyntaxLight.string,
    codeSyntaxKeywordLight: COLORS.codeSyntaxLight['keyword-light'],
    codeSyntaxPreprocessingDirective: COLORS.codeSyntaxLight['preprocessing-directive'],

    // checkbox
    checkboxHover: COLORS.indigo[50],
    checkboxCheckedHover: primary.light,
    checkboxDisabled: secondary.light,
    checkboxDisabledChecked: secondary.default,
    checkboxLabel: COLORS.blueGrey[500],

    // input search
    searchHighlight: COLORS.tangerine[50],

    // input field
    inputBackground: COLORS.white,
    inputBorder: secondary.default,
    inputFocus: primary.light,
    inputDanger: danger.default,
    inputDangerFocus: danger.light,
    inputSuccess: COLORS.yellowGreen[500],
    inputSuccessFocus: COLORS.yellowGreen[400],
    inputDisabled: secondary.light,
    inputDisabledBorder: secondary.default,
    inputPlaceholder: secondary.dark,

    // required input
    inputRequired: danger.dark,

    // tooltip
    tooltipBackground: COLORS.blueGrey[600],
    tooltipSeparator: secondary.dark,
    tooltipHighlight: secondary.default,

    // avatar
    avatarBackground: COLORS.white,
    avatarBorder: COLORS.blueGrey[100],

    // badges
    badgeNew: COLORS.indigo[100],
    badgeDefault: COLORS.blueGrey[100],
    badgeDeleted: COLORS.red[100],
    badgeCounter: COLORS.blueGrey[100],
    badgeCounterFailed: COLORS.red[50],
    badgeCounterFailedBorder: COLORS.red[200],

    // pills
    pillDanger: COLORS.red[50],
    pillDangerBorder: COLORS.red[300],
    pillWarning: COLORS.yellow[50],
    pillWarningBorder: COLORS.yellow[300],
    pillInfo: COLORS.blue[50],
    pillInfoBorder: COLORS.blue[300],
    pillAccent: COLORS.indigo[50],
    pillAccentBorder: 'transparent',

    // input select
    selectOptionSelected: secondary.light,

    // breadcrumbs
    breadcrumb: 'transparent',

    // tab
    tabBorder: primary.light,

    // tabs
    tab: COLORS.blueGrey[400],
    tabSelected: primary.default,
    tabHover: COLORS.blueGrey[25],

    //table
    tableRowHover: COLORS.indigo[25],
    tableRowSelected: COLORS.indigo[300],

    // links
    linkDefault: primary.default,
    linkNaked: COLORS.blueGrey[700],
    linkActive: COLORS.indigo[600],
    linkDiscreet: 'currentColor',
    linkTooltipDefault: COLORS.indigo[200],
    linkTooltipActive: COLORS.indigo[100],
    linkBorder: COLORS.indigo[300],
    linkExternalIcon: COLORS.indigo[300],
    linkExternalIconActive: COLORS.indigo[400],
    contentLinkBorder: COLORS.blueGrey[200],

    // discreet select
    discreetBorder: secondary.default,
    discreetBackground: COLORS.white,
    discreetHover: secondary.light,
    discreetButtonHover: COLORS.indigo[500],
    discreetFocus: COLORS.indigo[50],
    discreetFocusBorder: primary.light,

    // interactive icon
    interactiveIcon: 'transparent',
    interactiveIconHover: COLORS.indigo[50],
    interactiveIconFocus: primary.default,
    bannerIcon: 'transparent',
    bannerIconHover: [...COLORS.red[600], OPACITY_20_PERCENT],
    bannerIconFocus: danger.default,
    discreetInteractiveIcon: secondary.dark,
    destructiveIcon: 'transparent',
    destructiveIconHover: danger.lightest,
    destructiveIconFocus: danger.default,

    // icons
    iconSoftwareImpactSeverityHigh: COLORS.red[500],
    iconSoftwareImpactSeverityMedium: COLORS.yellow[700],
    iconSoftwareImpactSeverityLow: COLORS.blue[700],
    iconSoftwareImpactSeverityDisabled: COLORS.blueGrey[300],
    iconSeverityMajor: danger.light,
    iconSeverityMinor: COLORS.yellowGreen[400],
    iconSeverityInfo: COLORS.blue[400],
    iconSeverityDisabled: COLORS.blueGrey[300],
    iconTypeDisabled: COLORS.blueGrey[300],
    iconDirectory: COLORS.orange[300],
    iconFile: COLORS.blueGrey[300],
    iconProject: COLORS.blueGrey[300],
    iconUnitTest: COLORS.blueGrey[300],
    iconFavorite: COLORS.tangerine[400],
    iconCheck: COLORS.green[500],
    iconPositiveUpdate: COLORS.green[300],
    iconNegativeUpdate: COLORS.red[300],
    iconTrendPositive: COLORS.green[400],
    iconTrendNegative: COLORS.red[400],
    iconTrendNeutral: COLORS.blue[400],
    iconTrendDisabled: COLORS.blueGrey[400],
    iconError: danger.default,
    iconWarning: COLORS.yellow[600],
    iconSuccess: COLORS.green[600],
    iconInfo: COLORS.blue[600],
    iconStatus: COLORS.blueGrey[200],
    iconStatusResolved: secondary.dark,
    iconNotificationsOn: COLORS.indigo[300],
    iconHelperHint: COLORS.blueGrey[100],
    iconHelperHintRaised: COLORS.blueGrey[400],
    iconRuleInheritanceOverride: danger.light,

    // numbered list
    numberedList: COLORS.indigo[50],
    numberedListText: COLORS.indigo[800],

    // unordered list
    listMarker: COLORS.blueGrey[300],

    // product news
    productNews: COLORS.indigo[50],
    productNewsHover: COLORS.indigo[100],

    // scrollbar
    scrollbar: COLORS.blueGrey[25],

    // resizer
    resizer: secondary.default,

    // coverage indicators
    coverageGreen: COLORS.green[500],
    coverageRed: danger.dark,

    // duplications indicators
    'duplicationsIndicator.A': COLORS.green[500],
    'duplicationsIndicator.B': COLORS.green[500],
    'duplicationsIndicator.C': COLORS.yellowGreen[500],
    'duplicationsIndicator.D': COLORS.yellow[500],
    'duplicationsIndicator.E': COLORS.orange[500],
    'duplicationsIndicator.F': COLORS.red[500],
    duplicationsIndicatorSecondary: secondary.light,

    // size indicators
    sizeIndicator: COLORS.blue[500],

    // rating colors
    'rating.A': COLORS.green[200],
    'rating.B': COLORS.yellowGreen[200],
    'rating.C': COLORS.yellow[200],
    'rating.D': COLORS.orange[200],
    'rating.E': COLORS.red[200],

    // rating donut outside circle indicators
    'ratingDonut.A': COLORS.green[400],
    'ratingDonut.B': COLORS.yellowGreen[400],
    'ratingDonut.C': COLORS.yellow[400],
    'ratingDonut.D': COLORS.orange[400],
    'ratingDonut.E': COLORS.red[400],

    // date picker
    datePicker: COLORS.white,
    datePickerIcon: secondary.default,
    datePickerDisabled: COLORS.white,
    datePickerDefault: COLORS.white,
    datePickerHover: COLORS.blueGrey[100],
    datePickerSelected: primary.default,
    datePickerRange: COLORS.indigo[100],

    // tags
    tag: secondary.light,

    // quality gate indicator
    qgIndicatorPassed: COLORS.green[200],
    qgIndicatorFailed: COLORS.red[200],
    qgIndicatorNotComputed: COLORS.blueGrey[200],

    // quality gate status card
    qgCardFailed: COLORS.red[300],

    // quality gate texts colors
    qgConditionNotCayc: COLORS.red[600],
    qgConditionCayc: COLORS.green[600],

    // main bar
    mainBar: COLORS.white,
    mainBarHover: COLORS.blueGrey[600],
    mainBarLogo: COLORS.white,
    mainBarDarkLogo: COLORS.blueGrey[800],
    mainBarNews: COLORS.indigo[50],
    menuBorder: primary.light,

    // navbar
    navbar: COLORS.white,
    navbarTextMeta: secondary.darker,

    // filterbar
    filterbar: COLORS.white,
    filterbarBorder: COLORS.blueGrey[100],

    // facets
    facetHeader: COLORS.blueGrey[600],
    facetHeaderDisabled: COLORS.blueGrey[400],
    facetItemSelected: COLORS.indigo[50],
    facetItemSelectedHover: COLORS.indigo[100],
    facetItemSelectedBorder: primary.light,
    facetItemDisabled: COLORS.blueGrey[300],
    facetItemLight: secondary.dark,
    facetItemGraph: secondary.default,
    facetKeyboardHint: COLORS.blueGrey[50],
    facetToggleActive: COLORS.green[500],
    facetToggleInactive: COLORS.red[500],
    facetToggleHover: COLORS.blueGrey[600],

    // subnavigation sidebar
    subnavigation: COLORS.white,
    subnavigationHover: COLORS.blueGrey[50],
    subnavigationSelected: COLORS.blueGrey[100],
    subnavigationBorder: COLORS.grey[100],
    subnavigationSeparator: COLORS.grey[50],
    subnavigationSubheading: COLORS.blueGrey[25],
    subnavigationDisabled: COLORS.blueGrey[300],
    subnavigationExecutionFlow: COLORS.blueGrey[25],
    subnavigationExecutionFlowBorder: secondary.default,
    subnavigationExecutionFlowSeparator: COLORS.blueGrey[100],
    subnavigationExecutionFlowActive: COLORS.indigo[500],

    // footer
    footer: COLORS.white,
    footerBorder: COLORS.grey[100],

    // project
    projectCardBackground: COLORS.white,
    projectCardBorder: COLORS.blueGrey[100],

    // overview
    overviewCardDefaultIcon: secondary.light,
    iconOverviewIssue: COLORS.blueGrey[400],
    overviewCardWarningIcon: COLORS.yellow[50],
    overviewCardErrorIcon: COLORS.red[100],
    overviewCardSuccessIcon: COLORS.green[200],

    // overview software impact breakdown
    overviewSoftwareImpactSeverityNeutral: COLORS.blueGrey[50],
    overviewSoftwareImpactSeverityHigh: COLORS.red[100],
    overviewSoftwareImpactSeverityMedium: COLORS.yellow[100],
    overviewSoftwareImpactSeverityLow: COLORS.blue[100],

    // graph - chart
    graphPointCircleColor: COLORS.white,
    'graphLineColor.0': COLORS.blue[500],
    'graphLineColor.1': COLORS.blue[700],
    'graphLineColor.2': COLORS.blue[300],
    'graphLineColor.3': COLORS.blue[500],
    'graphLineColor.4': COLORS.blue[700],
    'graphLineColor.5': COLORS.blue[300],
    graphGridColor: COLORS.grey[50],
    graphCursorLineColor: COLORS.blueGrey[400],
    newCodeHighlight: COLORS.indigo[300],
    graphZoomBackgroundColor: COLORS.blueGrey[25],
    graphZoomBorderColor: COLORS.blueGrey[100],
    graphZoomHandleColor: COLORS.blueGrey[400],
    graphLegendBorder: secondary.darker,

    // page
    pageTitle: COLORS.blueGrey[700],
    pageContentLight: secondary.dark,
    pageContent: secondary.darker,
    pageContentDark: COLORS.blueGrey[600],
    pageBlock: COLORS.white,
    pageBlockBorder: COLORS.blueGrey[100],

    // core concepts
    coreConceptsCloseIcon: COLORS.blueGrey[300],
    coreConceptsTitle: secondary.darker,
    coreConceptsBody: secondary.darker,
    coreConceptsHomeBorder: COLORS.blueGrey[100],
    coreConceptsCompleted: COLORS.green[500],
    coreConceptsPulse: COLORS.indigo[500],
    coreConceptsPulseFallback: COLORS.white,

    // progress bar
    coreConceptsProgressBar: secondary.light,

    // issue box
    issueBoxSelectedBorder: danger.lighter,
    issueBoxBorder: secondary.default,
    issueBoxBorderDepracated: secondary.default,
    issueTypeIcon: COLORS.red[200],

    // separator
    pipeSeparator: COLORS.blueGrey[100],

    // drilldown link
    drilldown: secondary.darker,
    drilldownBorder: secondary.default,

    // selection card
    selectionCardHeader: secondary.darker,
    selectionCardDisabled: secondary.light,
    selectionCardDisabledText: secondary.dark,
    selectionCardBorder: COLORS.blueGrey[100],
    selectionCardBorderHover: COLORS.indigo[200],
    selectionCardBorderSelected: primary.light,
    selectionCardBorderDisabled: secondary.default,

    // bubble charts
    bubbleChartLine: COLORS.grey[50],
    bubbleDefault: [...COLORS.blue[500], 0.3],
    'bubble.1': [...COLORS.green[500], 0.3],
    'bubble.2': [...COLORS.yellowGreen[500], 0.3],
    'bubble.3': [...COLORS.yellow[500], 0.3],
    'bubble.4': [...COLORS.orange[500], 0.3],
    'bubble.5': [...COLORS.red[500], 0.3],

    // new code legend
    newCodeLegend: [...COLORS.indigo[300], 0.15],
    newCodeLegendBorder: COLORS.indigo[200],

    // highlighted section
    highlightedSection: COLORS.blueGrey[25],
    highlightedSectionBorder: COLORS.blueGrey[100],

    // highlight ring
    highlightRingBackground: secondary.light,

    // activity comments
    activityCommentPipe: COLORS.tangerine[200],

    // illustrations
    illustrationOutline: COLORS.blueGrey[400],
    illustrationInlineBorder: COLORS.blueGrey[100],
    illustrationPrimary: COLORS.indigo[400],
    illustrationSecondary: COLORS.indigo[200],
    illustrationShade: COLORS.indigo[25],

    // news bar
    newsBar: COLORS.white,
    newsBorder: COLORS.grey[100],
    newsContent: COLORS.white,
    newsTag: COLORS.blueGrey[50],
    roadmap: COLORS.indigo[25],
    roadmapContent: 'transparent',

    // project analyse page
    almCardBorder: COLORS.grey[100],

    // Keyboard hint
    keyboardHintKey: COLORS.blueGrey[100],

    // progressBar
    progressBarForeground: COLORS.indigo[500],
    progressBarBackground: COLORS.indigo[100],
  },

  // contrast colors to be used for text when using a color background with the same name
  // must match the color name
  contrasts: {
    backgroundPrimary: COLORS.blueGrey[900],
    backgroundSecondary: COLORS.blueGrey[900],
    primaryLight: secondary.darker,
    primary: COLORS.white,

    // switch
    switchHover: primary.light,
    switchButton: primary.default,
    switchButtonDisabled: COLORS.blueGrey[300],

    // sidebar
    sidebarBackground: COLORS.blueGrey[200],
    sidebarItemActive: COLORS.blueGrey[25],

    // flag message
    flagMessageBackground: secondary.darker,

    // info message
    infoBackground: COLORS.blue[900],

    // banner message
    bannerMessage: COLORS.red[900],

    // buttons
    buttonDisabled: COLORS.blueGrey[300],
    buttonSecondary: secondary.darker,

    // danger buttons
    dangerButton: COLORS.white,
    dangerButtonSecondary: danger.dark,

    // third party button
    thirdPartyButton: secondary.darker,

    // popup
    popup: secondary.darker,

    // dropdown menu
    dropdownMenu: secondary.darker,
    dropdownMenuDisabled: COLORS.blueGrey[300],
    dropdownMenuHeader: secondary.dark,

    // toggle buttons
    toggle: secondary.darker,
    toggleHover: secondary.darker,

    // code viewer
    codeLineNewCodeUnderline: COLORS.indigo[500],
    codeLineCoveredUnderline: COLORS.green[700],
    codeLineUncoveredUnderline: COLORS.red[700],
    codeLineEllipsis: COLORS.blueGrey[300],
    codeLineEllipsisHover: secondary.dark,
    codeLineLocationMarker: COLORS.red[900],
    codeLineLocationMarkerSelected: COLORS.red[900],
    codeLineIssueMessageTooltip: COLORS.blueGrey[25],

    // code snippet
    codeSnippetHighlight: danger.default,

    // checkbox
    checkboxDisabled: secondary.default,

    // input search
    searchHighlight: secondary.darker,

    // input field
    inputBackground: secondary.darker,
    inputDisabled: COLORS.blueGrey[300],

    // tooltip
    tooltipBackground: secondary.light,

    // badges
    badgeNew: COLORS.indigo[900],
    badgeDefault: COLORS.blueGrey[700],
    badgeDeleted: COLORS.red[900],
    badgeCounter: secondary.darker,
    badgeCounterFailed: danger.dark,

    // pills
    pillDanger: COLORS.red[800],
    pillDangerIcon: COLORS.red[700],
    pillWarning: COLORS.yellow[800],
    pillWarningIcon: COLORS.yellow[700],
    pillInfo: COLORS.blue[800],
    pillInfoIcon: COLORS.blue[700],
    pillAccent: COLORS.indigo[500],

    // project cards
    overviewCardDefaultIcon: COLORS.blueGrey[500],
    overviewCardWarningIcon: COLORS.yellow[700],
    overviewCardErrorIcon: COLORS.red[500],
    overviewCardSuccessIcon: COLORS.green[500],

    // breadcrumbs
    breadcrumb: secondary.dark,

    // discreet select
    discreetBackground: secondary.darker,
    discreetHover: secondary.darker,

    // interactive icons
    interactiveIcon: primary.dark,
    interactiveIconHover: COLORS.indigo[800],
    bannerIcon: danger.darker,
    bannerIconHover: danger.darker,
    destructiveIcon: danger.default,
    destructiveIconHover: danger.darker,

    // icons
    iconSeverityMajor: COLORS.white,
    iconSeverityMinor: COLORS.white,
    iconSeverityInfo: COLORS.white,
    iconStatusResolved: COLORS.white,
    iconHelperHint: secondary.darker,
    iconHelperHintRaised: COLORS.white,

    // numbered list
    numberedList: COLORS.indigo[800],

    // product news
    productNews: secondary.darker,
    productNewsHover: secondary.darker,

    // scrollbar
    scrollbar: COLORS.grey[100],

    // size indicators
    sizeIndicator: COLORS.white,

    // rating colors
    'rating.A': COLORS.green[900],
    'rating.B': COLORS.yellowGreen[900],
    'rating.C': COLORS.yellow[900],
    'rating.D': COLORS.orange[900],
    'rating.E': COLORS.red[900],

    // date picker
    datePicker: COLORS.blueGrey[300],
    datePickerDisabled: COLORS.blueGrey[300],
    datePickerDefault: COLORS.blueGrey[600],
    datePickerHover: COLORS.blueGrey[600],
    datePickerSelected: COLORS.white,
    datePickerRange: COLORS.blueGrey[600],

    // tags
    tag: secondary.darker,

    // quality gate indicator
    qgIndicatorPassed: COLORS.green[800],
    qgIndicatorFailed: danger.darker,
    qgIndicatorNotComputed: COLORS.blueGrey[800],

    // main bar
    mainBar: secondary.darker,
    mainBarLogo: COLORS.black,
    mainBarDarkLogo: COLORS.white,
    mainBarNews: secondary.darker,

    // navbar
    navbar: secondary.darker,

    // filterbar
    filterbar: secondary.darker,

    // facet
    facetKeyboardHint: secondary.darker,
    facetToggleActive: COLORS.white,
    facetToggleInactive: COLORS.white,

    // subnavigation sidebar
    subnavigation: secondary.darker,
    subnavigationExecutionFlow: COLORS.blueGrey[700],
    subnavigationHover: COLORS.blueGrey[700],
    subnavigationSubheading: secondary.dark,

    // footer
    footer: secondary.dark,

    // page
    pageBlock: secondary.darker,

    // graph - chart
    graphZoomHandleColor: COLORS.white,

    // progress bar
    coreConceptsProgressBar: primary.light,

    // issue box
    issueTypeIcon: COLORS.red[900],
    iconSeverityDisabled: COLORS.white,
    iconTypeDisabled: COLORS.white,

    // selection card
    selectionCardDisabled: secondary.dark,

    // bubble charts
    bubbleDefault: COLORS.blue[500],
    'bubble.1': COLORS.green[500],
    'bubble.2': COLORS.yellowGreen[500],
    'bubble.3': COLORS.yellow[500],
    'bubble.4': COLORS.orange[500],
    'bubble.5': COLORS.red[500],

    // news bar
    newsBar: COLORS.blueGrey[600],
    newsContent: COLORS.blueGrey[500],
    newsTag: COLORS.blueGrey[500],
    roadmap: COLORS.blueGrey[600],
    roadmapContent: COLORS.blueGrey[500],

    // Keyboard hint
    keyboardHintKey: COLORS.blueGrey[500],
  },

  // predefined shadows
  shadows: {
    xs: [[0, 1, 2, 0, ...COLORS.blueGrey[700], 0.05]],
    sm: [
      [0, 1, 3, 0, ...COLORS.blueGrey[700], 0.05],
      [0, 1, 25, 0, ...COLORS.blueGrey[700], 0.05],
    ],
    md: [
      [0, 4, 8, -2, ...COLORS.blueGrey[700], 0.1],
      [0, 2, 15, -2, ...COLORS.blueGrey[700], 0.06],
    ],
    lg: [
      [0, 12, 16, -4, ...COLORS.blueGrey[700], 0.1],
      [0, 4, 6, -2, ...COLORS.blueGrey[700], 0.05],
    ],
    xl: [
      [15, 20, 24, -4, ...COLORS.blueGrey[700], 0.1],
      [0, 8, 8, -4, ...COLORS.blueGrey[700], 0.06],
    ],
  },

  // predefined borders
  borders: {
    default: ['1px', 'solid', ...COLORS.grey[50]],
    active: ['4px', 'solid', ...primary.light],
    xsActive: ['3px', 'solid', ...primary.light],
    focus: ['4px', 'solid', ...secondary.default, OPACITY_20_PERCENT],
    heavy: ['2px', 'solid', ...COLORS.grey[50]],
  },

  avatar: {
    color: [
      COLORS.blueGrey[100],
      COLORS.indigo[100],
      COLORS.tangerine[100],
      COLORS.green[100],
      COLORS.yellowGreen[100],
      COLORS.yellow[100],
      COLORS.orange[100],
      COLORS.red[100],
      COLORS.blue[100],
    ],
    contrast: [
      COLORS.blueGrey[900],
      COLORS.indigo[900],
      COLORS.tangerine[900],
      COLORS.green[900],
      COLORS.yellowGreen[900],
      COLORS.yellow[900],
      COLORS.orange[900],
      COLORS.red[900],
      COLORS.blue[900],
    ],
  },

  // Theme specific icons and images
  images: {
    azure: 'azure.svg',
    bitbucket: 'bitbucket.svg',
    github: 'github.svg',
    gitlab: 'gitlab.svg',
    microsoft: 'microsoft.svg',
    'cayc-1': 'cayc-1-light.gif',
    'cayc-2': 'cayc-2-light.gif',
    'cayc-3': 'cayc-3-light.svg',
    'cayc-4': 'cayc-4-light.svg',
    'new-code-1': 'new-code-1.svg',
    'new-code-2': 'new-code-2-light.svg',
    'new-code-3': 'new-code-3.gif',
    'new-code-4': 'new-code-4.gif',
    'new-code-5': 'new-code-5.png',
    'pull-requests-1': 'pull-requests-1-light.gif',
    'pull-requests-2': 'pull-requests-2-light.svg',
    'pull-requests-3': 'pull-requests-3.svg',
    'quality-gate-1': 'quality-gate-1.png',
    'quality-gate-2a': 'quality-gate-2a.svg',
    'quality-gate-2b': 'quality-gate-2b.png',
    'quality-gate-2c': 'quality-gate-2c.png',
    'quality-gate-3': 'quality-gate-3-light.svg',
    'quality-gate-4': 'quality-gate-4.png',
    'quality-gate-5': 'quality-gate-5.svg',

    // project configure page
    AzurePipe: '/images/alms/azure.svg',
    BitbucketPipe: '/images/alms/bitbucket.svg',
    BitbucketAzure: '/images/alms/azure.svg',
    BitbucketCircleCI: '/images/tutorials/circleci.svg',
    GitHubActions: '/images/alms/github.svg',
    GitHubCircleCI: '/images/tutorials/circleci.svg',
    GitHubTravis: '/images/tutorials/TravisCI-Mascot.png',
    GitLabPipeline: '/images/alms/gitlab.svg',
  },
};
