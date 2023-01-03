/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import * as reactIntl from 'react-intl';
import SonarUiCommonInitializer from 'sonar-ui-common/helpers/init';
import { get } from 'sonar-ui-common/helpers/storage';
import { fetchL10nBundle } from '../../api/l10n';
import { loadL10nBundle } from '../l10n';

beforeEach(() => {
  jest.clearAllMocks();
  jest.spyOn(window.navigator, 'languages', 'get').mockReturnValue(['de']);
});

jest.mock('../../api/l10n', () => ({
  fetchL10nBundle: jest
    .fn()
    .mockResolvedValue({ effectiveLocale: 'de', messages: { test_message: 'test' } })
}));

jest.mock('sonar-ui-common/helpers/storage', () => ({
  get: jest.fn(),
  save: jest.fn()
}));

describe('#loadL10nBundle', () => {
  it('should fetch bundle without any timestamp', async () => {
    await loadL10nBundle();

    expect(fetchL10nBundle).toHaveBeenCalledWith({ locale: 'de', ts: undefined });
  });

  it('should ftech bundle without local storage timestamp if locales are different', async () => {
    const cachedBundle = { timestamp: 'timestamp', locale: 'fr', messages: { cache: 'cache' } };
    (get as jest.Mock).mockReturnValueOnce(JSON.stringify(cachedBundle));

    await loadL10nBundle();

    expect(fetchL10nBundle).toHaveBeenCalledWith({ locale: 'de', ts: undefined });
  });

  it('should fetch bundle with cached bundle timestamp and browser locale', async () => {
    const cachedBundle = { timestamp: 'timestamp', locale: 'de', messages: { cache: 'cache' } };
    (get as jest.Mock).mockReturnValueOnce(JSON.stringify(cachedBundle));

    await loadL10nBundle();

    expect(fetchL10nBundle).toHaveBeenCalledWith({ locale: 'de', ts: cachedBundle.timestamp });
  });

  it('should fallback to cached bundle if the server respond with 304', async () => {
    const cachedBundle = { timestamp: 'timestamp', locale: 'fr', messages: { cache: 'cache' } };
    (fetchL10nBundle as jest.Mock).mockRejectedValueOnce({ status: 304 });
    (get as jest.Mock).mockReturnValueOnce(JSON.stringify(cachedBundle));

    const bundle = await loadL10nBundle();

    expect(bundle).toEqual(
      expect.objectContaining({ locale: cachedBundle.locale, messages: cachedBundle.messages })
    );
  });

  it('should init react-intl & sonar-ui-common', async () => {
    jest.spyOn(SonarUiCommonInitializer, 'setLocale');
    jest.spyOn(SonarUiCommonInitializer, 'setMessages');
    jest.spyOn(reactIntl, 'addLocaleData');

    await loadL10nBundle();

    expect(SonarUiCommonInitializer.setLocale).toHaveBeenCalledWith('de');
    expect(SonarUiCommonInitializer.setMessages).toHaveBeenCalledWith({ test_message: 'test' });
    expect(reactIntl.addLocaleData).toHaveBeenCalled();
  });
});
