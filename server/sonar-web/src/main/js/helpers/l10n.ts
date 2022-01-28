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
import { fetchL10nBundle } from '../api/l10n';
import { L10nBundle, L10nBundleRequestParams } from '../types/l10n';
import { Dict } from '../types/types';
import { toNotSoISOString } from './dates';
import { get as loadFromLocalStorage, save as saveInLocalStorage } from './storage';

export type Messages = Dict<string>;

export const DEFAULT_LOCALE = 'en';
export const DEFAULT_MESSAGES = {
  // eslint-disable-next-line camelcase
  default_error_message: 'The request cannot be processed. Try again later.'
};

let allMessages: Messages = {};
let locale: string | undefined;

export function translate(...keys: string[]): string {
  const messageKey = keys.join('.');
  const l10nMessages = getMessages();
  if (process.env.NODE_ENV === 'development' && !l10nMessages[messageKey]) {
    // eslint-disable-next-line no-console
    console.error(`No message for: ${messageKey}`);
  }
  return l10nMessages[messageKey] || messageKey;
}

export function translateWithParameters(
  messageKey: string,
  ...parameters: Array<string | number>
): string {
  const message = getMessages()[messageKey];
  if (message) {
    return parameters
      .map(parameter => String(parameter))
      .reduce((acc, parameter, index) => acc.replace(`{${index}}`, () => parameter), message);
  }
  if (process.env.NODE_ENV === 'development') {
    // eslint-disable-next-line no-console
    console.error(`No message for: ${messageKey}`);
  }
  return `${messageKey}.${parameters.join('.')}`;
}

export function hasMessage(...keys: string[]): boolean {
  const messageKey = keys.join('.');
  return getMessages()[messageKey] != null;
}

export function getMessages() {
  if (typeof allMessages === 'undefined') {
    logWarning('L10n messages are not initialized. Use default messages.');
    return DEFAULT_MESSAGES;
  }
  return allMessages;
}

export function resetMessages(newMessages: Messages) {
  allMessages = newMessages;
}

export function getLocalizedMetricName(
  metric: { key: string; name?: string },
  short = false
): string {
  const bundleKey = `metric.${metric.key}.${short ? 'short_name' : 'name'}`;
  if (hasMessage(bundleKey)) {
    return translate(bundleKey);
  } else if (short) {
    return getLocalizedMetricName(metric);
  }
  return metric.name || metric.key;
}

export function getLocalizedCategoryMetricName(metric: { key: string; name?: string }) {
  const bundleKey = `metric.${metric.key}.extra_short_name`;
  return hasMessage(bundleKey) ? translate(bundleKey) : getLocalizedMetricName(metric, true);
}

export function getLocalizedMetricDomain(domainName: string) {
  const bundleKey = `metric_domain.${domainName}`;
  return hasMessage(bundleKey) ? translate(bundleKey) : domainName;
}

export function getCurrentLocale() {
  return locale;
}

export function resetCurrentLocale(newLocale: string) {
  locale = newLocale;
}

export function getShortMonthName(index: number) {
  const months = [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'May',
    'Jun',
    'Jul',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec'
  ];
  return translate(months[index]);
}

export function getWeekDayName(index: number) {
  const weekdays = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  return weekdays[index] ? translate(weekdays[index]) : '';
}

export function getShortWeekDayName(index: number) {
  const weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  return weekdays[index] ? translate(weekdays[index]) : '';
}

const L10N_BUNDLE_LS_KEY = 'l10n.bundle';

export async function loadL10nBundle() {
  const bundle = await getLatestL10nBundle().catch(() => ({
    locale: DEFAULT_LOCALE,
    messages: {}
  }));

  resetCurrentLocale(bundle.locale);
  resetMessages(bundle.messages);

  return bundle;
}

export async function getLatestL10nBundle() {
  const browserLocale = getPreferredLanguage();
  const cachedBundle = loadL10nBundleFromLocalStorage();

  const params: L10nBundleRequestParams = {};

  if (browserLocale) {
    params.locale = browserLocale;

    if (
      cachedBundle.locale &&
      browserLocale.startsWith(cachedBundle.locale) &&
      cachedBundle.timestamp &&
      cachedBundle.messages
    ) {
      params.ts = cachedBundle.timestamp;
    }
  }

  const { effectiveLocale, messages } = await fetchL10nBundle(params).catch(response => {
    if (response && response.status === 304) {
      return {
        effectiveLocale: cachedBundle.locale || browserLocale || DEFAULT_LOCALE,
        messages: cachedBundle.messages ?? {}
      };
    }
    throw new Error(`Unexpected status code: ${response.status}`);
  });

  const bundle = {
    timestamp: toNotSoISOString(new Date()),
    locale: effectiveLocale,
    messages
  };

  saveL10nBundleToLocalStorage(bundle);

  return bundle;
}

export function getCurrentL10nBundle() {
  return loadL10nBundleFromLocalStorage();
}

function getPreferredLanguage() {
  return window.navigator.languages ? window.navigator.languages[0] : window.navigator.language;
}

function loadL10nBundleFromLocalStorage() {
  let bundle: L10nBundle;

  try {
    bundle = JSON.parse(loadFromLocalStorage(L10N_BUNDLE_LS_KEY) ?? '{}');
  } catch {
    bundle = {};
  }

  return bundle;
}

function saveL10nBundleToLocalStorage(bundle: L10nBundle) {
  saveInLocalStorage(L10N_BUNDLE_LS_KEY, JSON.stringify(bundle));
}

function logWarning(message: string) {
  if (process.env.NODE_ENV !== 'production') {
    // eslint-disable-next-line no-console
    console.warn(message);
  }
}
