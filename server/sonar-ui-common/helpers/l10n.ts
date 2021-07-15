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
import { getLocale, getMessages } from './init';

export type Messages = T.Dict<string>;

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
      .map((parameter) => String(parameter))
      .reduce((acc, parameter, index) => acc.replace(`{${index}}`, () => parameter), message);
  } else {
    if (process.env.NODE_ENV === 'development') {
      // eslint-disable-next-line no-console
      console.error(`No message for: ${messageKey}`);
    }
    return `${messageKey}.${parameters.join('.')}`;
  }
}

export function hasMessage(...keys: string[]): boolean {
  const messageKey = keys.join('.');
  return getMessages()[messageKey] != null;
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
  } else {
    return metric.name || metric.key;
  }
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
  return getLocale();
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
    'Dec',
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
