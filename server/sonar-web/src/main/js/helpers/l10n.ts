/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { getJSON } from './request';
import { toNotSoISOString } from './dates';

interface LanguageBundle {
  [name: string]: string;
}

interface BundleRequestParams {
  locale?: string;
  ts?: string;
}

interface BundleRequestResponse {
  effectiveLocale: string;
  messages: LanguageBundle;
}

let messages: LanguageBundle = {};

export const DEFAULT_LANGUAGE = 'en';

export function translate(...keys: string[]): string {
  const messageKey = keys.join('.');
  if (process.env.NODE_ENV === 'development') {
    if (!messages[messageKey]) {
      // eslint-disable-next-line
      console.error(`No message for: ${messageKey}`);
    }
  }
  return messages[messageKey] || messageKey;
}

export function translateWithParameters(
  messageKey: string,
  ...parameters: Array<string | number>
): string {
  const message = messages[messageKey];
  if (message) {
    return parameters
      .map(parameter => String(parameter))
      .reduce((acc, parameter, index) => acc.replace(`{${index}}`, parameter), message);
  } else {
    if (process.env.NODE_ENV === 'development') {
      // eslint-disable-next-line
      console.error(`No message for: ${messageKey}`);
    }
    return `${messageKey}.${parameters.join('.')}`;
  }
}

export function hasMessage(...keys: string[]): boolean {
  const messageKey = keys.join('.');
  return messages[messageKey] != null;
}

function getPreferredLanguage(): string | undefined {
  return window.navigator.languages ? window.navigator.languages[0] : window.navigator.language;
}

function checkCachedBundle(): boolean {
  const cached = localStorage.getItem('l10n.bundle');

  if (!cached) {
    return false;
  }

  try {
    const parsed = JSON.parse(cached);
    return parsed != null && typeof parsed === 'object';
  } catch (e) {
    return false;
  }
}

function getL10nBundle(params: BundleRequestParams): Promise<BundleRequestResponse> {
  const url = '/api/l10n/index';
  return getJSON(url, params);
}

export function requestMessages(): Promise<string> {
  const browserLocale = getPreferredLanguage();
  const cachedLocale = localStorage.getItem('l10n.locale');
  const params: BundleRequestParams = {};

  if (browserLocale) {
    params.locale = browserLocale;

    if (cachedLocale && browserLocale.startsWith(cachedLocale)) {
      const bundleTimestamp = localStorage.getItem('l10n.timestamp');
      if (bundleTimestamp !== null && checkCachedBundle()) {
        params.ts = bundleTimestamp;
      }
    }
  }

  return getL10nBundle(params).then(
    ({ effectiveLocale, messages }: BundleRequestResponse) => {
      try {
        const currentTimestamp = toNotSoISOString(new Date());
        localStorage.setItem('l10n.timestamp', currentTimestamp);
        localStorage.setItem('l10n.locale', effectiveLocale);
        localStorage.setItem('l10n.bundle', JSON.stringify(messages));
      } catch (e) {
        // do nothing
      }
      resetBundle(messages);
      return effectiveLocale;
    },
    ({ response }) => {
      if (response && response.status === 304) {
        resetBundle(JSON.parse(localStorage.getItem('l10n.bundle') || '{}'));
      } else {
        throw new Error('Unexpected status code: ' + response.status);
      }
      return cachedLocale || browserLocale || DEFAULT_LANGUAGE;
    }
  );
}

export function resetBundle(bundle: LanguageBundle) {
  messages = bundle;
}

export function installGlobal() {
  (window as any).t = translate;
  (window as any).tp = translateWithParameters;
  (window as any).requestMessages = requestMessages;
}

export function getLocalizedDashboardName(baseName: string) {
  const l10nKey = `dashboard.${baseName}.name`;
  const l10nLabel = translate(l10nKey);
  return l10nLabel !== l10nKey ? l10nLabel : baseName;
}

export function getLocalizedMetricName(
  metric: { key: string; name?: string },
  short?: boolean
): string {
  const bundleKey = `metric.${metric.key}.${short ? 'short_name' : 'name'}`;
  const fromBundle = translate(bundleKey);
  if (fromBundle === bundleKey) {
    if (short) {
      return getLocalizedMetricName(metric);
    }
    return metric.name || metric.key;
  }
  return fromBundle;
}

export function getLocalizedCategoryMetricName(metric: { key: string; name?: string }) {
  const bundleKey = `metric.${metric.key}.extra_short_name`;
  const fromBundle = translate(bundleKey);
  return fromBundle === bundleKey ? getLocalizedMetricName(metric, true) : fromBundle;
}

export function getLocalizedMetricDomain(domainName: string) {
  const bundleKey = `metric_domain.${domainName}`;
  const fromBundle = translate(bundleKey);
  return fromBundle !== bundleKey ? fromBundle : domainName;
}

export function getCurrentLocale() {
  // check `window && window.localStorage` for tests
  return (
    (window && window.localStorage && window.localStorage.getItem('l10n.locale')) ||
    DEFAULT_LANGUAGE
  );
}
