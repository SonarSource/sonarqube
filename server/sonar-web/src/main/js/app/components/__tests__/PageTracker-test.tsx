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
import { shallow } from 'enzyme';
import * as React from 'react';
import { installScript } from '../../../helpers/extensions';
import { getWebAnalyticsPageHandlerFromCache } from '../../../helpers/extensionsHandler';
import { mockAppState, mockLocation } from '../../../helpers/testMocks';
import { PageTracker } from '../PageTracker';

jest.mock('../../../helpers/extensions', () => ({
  installScript: jest.fn().mockResolvedValue({}),
}));

jest.mock('../../../helpers/extensionsHandler', () => ({
  getWebAnalyticsPageHandlerFromCache: jest.fn().mockReturnValue(undefined),
}));

beforeAll(() => {
  jest.useFakeTimers();
});

afterAll(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

beforeEach(() => {
  jest.clearAllTimers();
  jest.clearAllMocks();
});

it('should not trigger if no analytics system is given', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(installScript).not.toHaveBeenCalled();
});

it('should work for WebAnalytics plugin', () => {
  const pageChange = jest.fn();
  const webAnalyticsJsPath = '/static/pluginKey/web_analytics.js';
  const wrapper = shallowRender({ appState: mockAppState({ webAnalyticsJsPath }) });

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('Helmet').prop('onChangeClientState')).toBe(wrapper.instance().trackPage);
  expect(installScript).toHaveBeenCalledWith(webAnalyticsJsPath, 'head');
  (getWebAnalyticsPageHandlerFromCache as jest.Mock).mockReturnValueOnce(pageChange);

  wrapper.instance().trackPage();
  jest.runAllTimers();
  expect(pageChange).toHaveBeenCalledWith('/path');
});

function shallowRender(props: Partial<PageTracker['props']> = {}) {
  return shallow<PageTracker>(
    <PageTracker appState={mockAppState()} location={mockLocation()} {...props} />
  );
}
