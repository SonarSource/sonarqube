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

import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { Link } from 'react-router-dom';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { installScript } from '../../../helpers/extensions';
import { getWebAnalyticsPageHandlerFromCache } from '../../../helpers/extensionsHandler';
import { mockAppState } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import PageTracker from '../PageTracker';

jest.mock('../../../helpers/extensions', () => ({
  installScript: jest.fn().mockResolvedValue({}),
}));

jest.mock('../../../helpers/extensionsHandler', () => ({
  getWebAnalyticsPageHandlerFromCache: jest.fn().mockReturnValue(undefined),
}));

beforeEach(() => {
  jest.clearAllMocks();
  jest.useFakeTimers();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

it('should not trigger if no analytics system is given', () => {
  renderPageTracker();

  expect(installScript).not.toHaveBeenCalled();
});

it('should work for WebAnalytics plugin', async () => {
  const user = userEvent.setup({ delay: null });
  const pageChange = jest.fn();
  const webAnalyticsJsPath = '/static/pluginKey/web_analytics.js';
  renderPageTracker(mockAppState({ webAnalyticsJsPath }));

  expect(installScript).toHaveBeenCalledWith(webAnalyticsJsPath, 'head');

  jest.mocked(getWebAnalyticsPageHandlerFromCache).mockClear().mockReturnValueOnce(pageChange);

  // trigger trackPage
  await user.click(byRole('link').get());

  jest.runAllTimers();
  expect(pageChange).toHaveBeenCalledWith('/newpath');
});

function renderPageTracker(appState = mockAppState()) {
  return renderComponent(<WrappingComponent />, '', { appState });
}

function WrappingComponent() {
  const [metatag, setmetatag] = React.useState<React.ReactNode>(null);

  return (
    <>
      <PageTracker>{metatag}</PageTracker>
      <Link to="newpath" onClick={() => setmetatag(<meta name="toto" />)}>
        trigger change
      </Link>
    </>
  );
}
