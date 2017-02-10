/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
/* @flow */
import moment from 'moment';
import { request } from './request';

let messages = {};

export function translate (...keys: string[]) {
  const messageKey = keys.join('.');
  return messages[messageKey] || messageKey;
}

export function translateWithParameters (messageKey: string, ...parameters: Array<string | number>) {
  const message = messages[messageKey];
  if (message) {
    return parameters
        .map(parameter => String(parameter))
        .reduce((acc, parameter, index) => acc.replace(`{${index}}`, parameter), message);
  } else {
    return `${messageKey}.${parameters.join('.')}`;
  }
}

export function hasMessage (...keys: string[]) {
  const messageKey = keys.join('.');
  return messages[messageKey] != null;
}

function getCurrentLocale () {
  return window.navigator.languages ? window.navigator.languages[0] : window.navigator.language;
}

function makeRequest (params) {
  const url = '/api/l10n/index';

  return request(url)
      .setData(params)
      .submit()
      .then(response => {
        switch (response.status) {
          case 200:
            return response.json();
          case 304:
            return JSON.parse(localStorage.getItem('l10n.bundle') || '{}');
          case 401:
            window.location = window.baseUrl + '/sessions/new?return_to=' +
                encodeURIComponent(window.location.pathname + window.location.search + window.location.hash);
            // return unresolved promise to stop the promise chain
            // anyway the page will be reloaded
            return new Promise(() => {});
          default:
            throw new Error('Unexpected status code: ' + response.status);
        }
      });
}

function checkCachedBundle () {
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

export function requestMessages () {
  const currentLocale = getCurrentLocale();
  const cachedLocale = localStorage.getItem('l10n.locale');

  if (cachedLocale !== currentLocale) {
    localStorage.removeItem('l10n.timestamp');
  }

  const bundleTimestamp = localStorage.getItem('l10n.timestamp');
  const params = {};
  if (currentLocale) {
    params.locale = currentLocale;
  }
  if (bundleTimestamp !== null && checkCachedBundle()) {
    params.ts = bundleTimestamp;
  }

  return makeRequest(params).then(bundle => {
    try {
      const currentTimestamp = moment().format('YYYY-MM-DDTHH:mm:ssZZ');
      localStorage.setItem('l10n.timestamp', currentTimestamp);
      localStorage.setItem('l10n.locale', currentLocale);
      localStorage.setItem('l10n.bundle', JSON.stringify(bundle));
    } catch (e) {
      // do nothing
    }

    messages = bundle;
  });
}

export function resetBundle (bundle: Object) {
  messages = bundle;
}

export function installGlobal () {
  window.t = translate;
  window.tp = translateWithParameters;
  window.requestMessages = requestMessages;
}

export function getLocalizedDashboardName (baseName: string) {
  const l10nKey = `dashboard.${baseName}.name`;
  const l10nLabel = translate(l10nKey);
  return l10nLabel !== l10nKey ? l10nLabel : baseName;
}

export function getLocalizedMetricName (metric: { key: string, name: string }) {
  const bundleKey = `metric.${metric.key}.name`;
  const fromBundle = translate(bundleKey);
  return fromBundle !== bundleKey ? fromBundle : metric.name;
}

export function getLocalizedMetricDomain (domainName: string) {
  const bundleKey = `metric_domain.${domainName}`;
  const fromBundle = translate(bundleKey);
  return fromBundle !== bundleKey ? fromBundle : domainName;
}
