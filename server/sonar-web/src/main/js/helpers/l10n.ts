/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { toNotSoISOString } from '../sonar-ui-common/helpers/dates';
import SonarUiCommonInitializer, { DEFAULT_LOCALE } from '../sonar-ui-common/helpers/init';
import {
  get as loadFromLocalStorage,
  save as saveInLocalStorage
} from '../sonar-ui-common/helpers/storage';
import { L10nBundle, L10nBundleRequestParams } from '../types/l10n';

const L10N_BUNDLE_LS_KEY = 'l10n.bundle';

export async function loadL10nBundle() {
  const bundle = await getLatestL10nBundle().catch(() => ({
    locale: DEFAULT_LOCALE,
    messages: {}
  }));

  SonarUiCommonInitializer.setLocale(bundle.locale).setMessages(bundle.messages);
  // No need to load english (default) bundle, it's coming with react-intl
  if (bundle.locale !== DEFAULT_LOCALE) {
    const [intlBundle, intl] = await Promise.all([
      import(`react-intl/locale-data/${bundle.locale}`),
      import('react-intl')
    ]);

    intl.addLocaleData(intlBundle.default);
  }

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
    } else {
      throw new Error(`Unexpected status code: ${response.status}`);
    }
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
