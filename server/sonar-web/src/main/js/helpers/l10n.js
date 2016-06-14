/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import moment from 'moment';
import { request } from './request';

let messages = {};

export function translate (...keys) {
  const messageKey = keys.join('.');
  return messages[messageKey] || messageKey;
}

export function translateWithParameters (messageKey, ...parameters) {
  const message = messages[messageKey];
  if (message) {
    return parameters.reduce((acc, parameter, index) => acc.replace(`{${index}}`, parameter), message);
  } else {
    return `${messageKey}.${parameters.join('.')}`;
  }
}

function getCurrentLocale () {
  return window.navigator.languages ? window.navigator.languages[0] : window.navigator.language;
}

function makeRequest (params) {
  const url = window.baseUrl + '/api/l10n/index';

  return request(url)
      .setData(params)
      .submit()
      .then(response => {
        if (response.status === 304) {
          return JSON.parse(localStorage.getItem('l10n.bundle'));
        } else if (response.status === 200) {
          return response.json();
        } else {
          throw new Error(response.status);
        }
      });
}

export function requestMessages () {
  const currentLocale = getCurrentLocale();
  const cachedLocale = localStorage.getItem('l10n.locale');

  if (cachedLocale !== currentLocale) {
    localStorage.removeItem('l10n.timestamp');
  }

  const bundleTimestamp = localStorage.getItem('l10n.timestamp');
  const params = { locale: currentLocale };
  if (bundleTimestamp !== null) {
    params.ts = bundleTimestamp;
  }

  return makeRequest(params).then(bundle => {
    const currentTimestamp = moment().format('YYYY-MM-DDTHH:mm:ssZZ');
    localStorage.setItem('l10n.timestamp', currentTimestamp);
    localStorage.setItem('l10n.locale', currentLocale);
    localStorage.setItem('l10n.bundle', JSON.stringify(bundle));
    messages = bundle;
  });
}

export function resetBundle (bundle) {
  messages = bundle;
}

export function installGlobal () {
  window.t = translate;
  window.tp = translateWithParameters;
  window.requestMessages = requestMessages;
}

export function getLocalizedDashboardName (baseName) {
  const l10nKey = `dashboard.${baseName}.name`;
  const l10nLabel = translate(l10nKey);
  return l10nLabel !== l10nKey ? l10nLabel : baseName;
}

export function getLocalizedMetricName (metric) {
  const bundleKey = `metric.${metric.key}.name`;
  const fromBundle = translate(bundleKey);
  return fromBundle !== bundleKey ? fromBundle : metric.name;
}

export function getLocalizedMetricDomain (domainName) {
  const bundleKey = `metric_domain.${domainName}`;
  const fromBundle = translate(bundleKey);
  return fromBundle !== bundleKey ? fromBundle : domainName;
}
