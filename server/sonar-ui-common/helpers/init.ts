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
import type { Messages } from './l10n';

let urlContext: string; // Is the base path (web context) in SQ
let messages: Messages | undefined;
let locale: string | undefined;
let reactDomContainerSelector: string | undefined; // CSS selector of the DOM node where the React App is attached

export const IS_SSR = typeof window === 'undefined';
export const DEFAULT_LOCALE = 'en';
export const DEFAULT_MESSAGES = {
  // eslint-disable-next-line camelcase
  default_error_message: 'The request cannot be processed. Try again later.',
};
const LOGGER_PREFIX = 'sonar-ui-common init:';

export default {
  setUrlContext(newUrlContext: string) {
    urlContext = newUrlContext;
    return this;
  },
  setLocale(newLocale: string) {
    locale = newLocale;
    return this;
  },
  setMessages(newMessages: Messages) {
    messages = newMessages;
    return this;
  },
  setReactDomContainer(nodeSelector: string) {
    reactDomContainerSelector = nodeSelector;
    return this;
  },
};

export function getMessages() {
  if (typeof messages === 'undefined') {
    logWarning('L10n messages are not initialized. Use default messages.');
    return DEFAULT_MESSAGES;
  }
  return messages;
}

export function getLocale() {
  if (typeof locale === 'undefined') {
    logWarning('L10n locale is not initialized. Use default locale.');
    return DEFAULT_LOCALE;
  }
  return locale;
}

export function getReactDomContainerSelector() {
  return reactDomContainerSelector || '#content';
}

export function getUrlContext() {
  if (typeof urlContext === 'undefined') {
    throw new Error(
      `${LOGGER_PREFIX} web context needs to be initialized by Initializer.setUrlContext before being used`
    );
  }
  return urlContext;
}

function logWarning(message: string) {
  if (process.env.NODE_ENV !== 'production') {
    // eslint-disable-next-line no-console
    console.warn(`${LOGGER_PREFIX} ${message}`);
  }
}
