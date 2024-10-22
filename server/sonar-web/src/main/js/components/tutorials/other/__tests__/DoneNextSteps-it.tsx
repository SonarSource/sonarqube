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

import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { EditionKey } from '../../../../types/editions';
import DoneNextSteps from '../DoneNextSteps';

const ui = {
  analysisDone: byText('onboarding.analysis.auto_refresh_after_analysis.done'),
  autoRefresh: byText('onboarding.analysis.auto_refresh_after_analysis.auto_refresh'),
  licensedNextStep: byText('onboarding.analysis.auto_refresh_after_analysis.check_these_links'),
  communityNextStep: byText(
    'onboarding.analysis.auto_refresh_after_analysis.community.check_these_links',
  ),
  nextStepLinks: byRole('link'),
};

describe('Community Edition', () => {
  it('should inform the user about available next steps', async () => {
    renderDoneNextSteps();

    expect(await ui.analysisDone.find()).toBeInTheDocument();
    expect(await ui.autoRefresh.find()).toBeInTheDocument();
    expect(await ui.communityNextStep.find()).toBeInTheDocument();
    expect(await ui.nextStepLinks.findAll()).toHaveLength(3);
  });
});

describe('Licensed Edition', () => {
  it('should inform the user about available next steps', async () => {
    renderDoneNextSteps(mockAppState({ edition: EditionKey.enterprise }));

    expect(await ui.analysisDone.find()).toBeInTheDocument();
    expect(await ui.autoRefresh.find()).toBeInTheDocument();
    expect(await ui.licensedNextStep.find()).toBeInTheDocument();
    expect(await ui.nextStepLinks.findAll()).toHaveLength(2);
  });
});

function renderDoneNextSteps(appState = mockAppState()) {
  return renderApp('/', <DoneNextSteps />, { appState });
}
