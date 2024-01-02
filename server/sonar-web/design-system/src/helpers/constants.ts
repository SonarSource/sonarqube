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
import { theme } from 'twin.macro';

export const DEFAULT_LOCALE = 'en';
export const IS_SSR = typeof window === 'undefined';
export const REACT_DOM_CONTAINER = '#content';

export const RULE_STATUSES = ['READY', 'BETA', 'DEPRECATED'];

export const THROTTLE_SCROLL_DELAY = 10;
export const THROTTLE_KEYPRESS_DELAY = 100;

export const DEBOUNCE_DELAY = 250;

export const DEBOUNCE_LONG_DELAY = 1000;

export const DEBOUNCE_SUCCESS_DELAY = 1000;

export const INTERACTIVE_TOOLTIP_DELAY = 0.5;

export const LEAK_PERIOD = 'sonar.leak.period';

export const LEAK_PERIOD_TYPE = 'sonar.leak.period.type';

export const INPUT_SIZES = {
  small: theme('width.input-small'),
  medium: theme('width.input-medium'),
  large: theme('width.input-large'),
  full: theme('width.full'),
  auto: theme('width.auto'),
};

export const LAYOUT_VIEWPORT_MIN_WIDTH = 1280;
export const LAYOUT_VIEWPORT_MAX_WIDTH = 1280;
export const LAYOUT_VIEWPORT_MAX_WIDTH_LARGE = 1680;
export const LAYOUT_MAIN_CONTENT_GUTTER = 60;
export const LAYOUT_SIDEBAR_WIDTH = 240;
export const LAYOUT_SIDEBAR_COLLAPSED_WIDTH = 60;
export const LAYOUT_SIDEBAR_BREAKPOINT = 1320;
export const LAYOUT_BANNER_HEIGHT = 44;
export const LAYOUT_BRANDING_ICON_WIDTH = 198;
export const LAYOUT_FILTERBAR_HEADER = 56;
export const LAYOUT_GLOBAL_NAV_HEIGHT = 52;
export const LAYOUT_PROJECT_NAV_HEIGHT = 108;
export const LAYOUT_LOGO_MARGIN_RIGHT = 45;
export const LAYOUT_LOGO_MAX_HEIGHT = 40;
export const LAYOUT_LOGO_MAX_WIDTH = 150;
export const LAYOUT_FOOTER_HEIGHT = 60;
export const LAYOUT_NOTIFICATIONSBAR_WIDTH = 350;

export const CORE_CONCEPTS_WIDTH = 350;

export const DARK_THEME_ID = 'dark-theme';

export const OPACITY_20_PERCENT = 0.2;

export const OPACITY_75_PERCENT = 0.75;

export const GLOBAL_POPUP_Z_INDEX = 5000;

export const TOAST_AUTOCLOSE_DELAY = 5000;
