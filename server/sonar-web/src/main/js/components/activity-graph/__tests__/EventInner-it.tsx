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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { Route } from 'react-router-dom';
import BranchesServiceMock from '../../../api/mocks/BranchesServiceMock';
import { isMainBranch } from '../../../helpers/branch-like';
import { mockBranch, mockMainBranch } from '../../../helpers/mocks/branch-like';
import { mockAnalysisEvent } from '../../../helpers/mocks/project-activity';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../helpers/testSelector';
import { Branch, BranchLike } from '../../../types/branch-like';
import {
  ApplicationAnalysisEventCategory,
  DefinitionChangeType,
  ProjectAnalysisEventCategory,
} from '../../../types/project-activity';
import EventInner, { EventInnerProps } from '../EventInner';

const ui = {
  showMoreBtn: byRole('button', { name: 'more' }),
  showLessBtn: byRole('button', { name: 'hide' }),
  projectLink: (name: string) => byRole('link', { name }),

  definitionChangeLabel: byText('event.category.DEFINITION_CHANGE', { exact: false }),
  projectAddedTxt: (branch: BranchLike) =>
    isMainBranch(branch)
      ? byText(/event\.definition_change\.added/)
      : byText(/event\.definition_change\.branch_added/),
  projectRemovedTxt: (branch: BranchLike) =>
    isMainBranch(branch)
      ? byText('event.definition_change.removed')
      : byText('event.definition_change.branch_removed'),
  branchReplacedTxt: byText('event.definition_change.branch_replaced'),

  qualityGateLabel: byText('event.category.QUALITY_GATE', { exact: false }),
  stillFailingTxt: byText('event.quality_gate.still_x'),

  versionLabel: byText('event.category.VERSION', { exact: false }),

  sqUpgradeLabel: (sqVersion: string) => byText(`event.sqUpgrade${sqVersion}`),
};

const handler = new BranchesServiceMock();

beforeEach(() => {
  handler.reset();
});

describe('DEFINITION_CHANGE events', () => {
  it.each([mockMainBranch(), mockBranch()])(
    'should render correctly for "ADDED" events',
    async (branchLike: Branch) => {
      handler.addBranch(branchLike);
      const user = userEvent.setup();
      renderEventInner(
        {
          event: mockAnalysisEvent({
            category: ApplicationAnalysisEventCategory.DefinitionChange,
            definitionChange: {
              projects: [
                {
                  changeType: DefinitionChangeType.Added,
                  key: 'foo',
                  name: 'Foo',
                  branch: 'master-foo',
                },
              ],
            },
          }),
        },
        `branch=${branchLike.name}&id=my-project`,
      );

      expect(await ui.definitionChangeLabel.find()).toBeInTheDocument();

      await user.click(ui.showMoreBtn.get());

      expect(await ui.projectAddedTxt(branchLike).find()).toBeInTheDocument();
      expect(ui.projectLink('Foo').get()).toBeInTheDocument();
      expect(screen.getByText('master-foo')).toBeInTheDocument();
    },
  );

  it.each([mockMainBranch(), mockBranch()])(
    'should render correctly for "REMOVED" events',
    async (branchLike: Branch) => {
      const user = userEvent.setup();
      handler.addBranch(branchLike);
      renderEventInner(
        {
          event: mockAnalysisEvent({
            category: ApplicationAnalysisEventCategory.DefinitionChange,
            definitionChange: {
              projects: [
                {
                  changeType: DefinitionChangeType.Removed,
                  key: 'bar',
                  name: 'Bar',
                  branch: 'master-bar',
                },
              ],
            },
          }),
        },
        `branch=${branchLike.name}&id=my-project`,
      );

      expect(ui.definitionChangeLabel.get()).toBeInTheDocument();

      await user.click(ui.showMoreBtn.get());

      expect(await ui.projectRemovedTxt(branchLike).find()).toBeInTheDocument();
      expect(ui.projectLink('Bar').get()).toBeInTheDocument();
      expect(screen.getByText('master-bar')).toBeInTheDocument();
    },
  );

  it('should render correctly for "BRANCH_CHANGED" events', async () => {
    const user = userEvent.setup();
    renderEventInner({
      event: mockAnalysisEvent({
        category: ApplicationAnalysisEventCategory.DefinitionChange,
        definitionChange: {
          projects: [
            {
              changeType: DefinitionChangeType.BranchChanged,
              key: 'baz',
              name: 'Baz',
              oldBranch: 'old-branch',
              newBranch: 'new-branch',
            },
          ],
        },
      }),
    });

    expect(ui.definitionChangeLabel.get()).toBeInTheDocument();

    await user.click(ui.showMoreBtn.get());

    expect(ui.branchReplacedTxt.get()).toBeInTheDocument();
    expect(ui.projectLink('Baz').get()).toBeInTheDocument();
    expect(screen.getByText('old-branch')).toBeInTheDocument();
    expect(screen.getByText('new-branch')).toBeInTheDocument();
  });
});

describe('QUALITY_GATE events', () => {
  it('should render correctly for simple "QUALITY_GATE" events', () => {
    renderEventInner({
      event: mockAnalysisEvent({
        category: ProjectAnalysisEventCategory.QualityGate,
        qualityGate: { status: 'ERROR', stillFailing: false, failing: [] },
      }),
    });

    expect(ui.qualityGateLabel.get()).toBeInTheDocument();
  });

  it('should render correctly for "still failing" "QUALITY_GATE" events', () => {
    renderEventInner({
      event: mockAnalysisEvent({
        category: ProjectAnalysisEventCategory.QualityGate,
        qualityGate: { status: 'ERROR', stillFailing: true, failing: [] },
      }),
    });

    expect(ui.qualityGateLabel.get()).toBeInTheDocument();
    expect(ui.stillFailingTxt.get()).toBeInTheDocument();
  });

  it('should render correctly for application "QUALITY_GATE" events', async () => {
    const user = userEvent.setup();
    renderEventInner({
      event: mockAnalysisEvent({
        category: ProjectAnalysisEventCategory.QualityGate,
        qualityGate: {
          status: 'ERROR',
          stillFailing: true,
          failing: [
            {
              key: 'foo',
              name: 'Foo',
              branch: 'master',
            },
            {
              key: 'bar',
              name: 'Bar',
              branch: 'feature/bar',
            },
          ],
        },
      }),
    });

    expect(ui.qualityGateLabel.get()).toBeInTheDocument();

    await user.click(ui.showMoreBtn.get());
    expect(ui.projectLink('Foo').get()).toBeInTheDocument();
    expect(ui.projectLink('Bar').get()).toBeInTheDocument();

    await user.click(ui.showLessBtn.get());
    expect(ui.projectLink('Foo').query()).not.toBeInTheDocument();
    expect(ui.projectLink('Bar').query()).not.toBeInTheDocument();
  });
});

describe('VERSION events', () => {
  it('should render correctly', () => {
    renderEventInner({
      event: mockAnalysisEvent({
        category: ProjectAnalysisEventCategory.Version,
        name: '1.0',
      }),
    });

    expect(ui.versionLabel.get()).toBeInTheDocument();
    expect(screen.getByText('1.0')).toBeInTheDocument();
  });
});

describe('SQ_UPGRADE events', () => {
  it('should render correctly', () => {
    renderEventInner({
      event: mockAnalysisEvent({
        category: ProjectAnalysisEventCategory.SqUpgrade,
        name: '10.0',
      }),
    });

    expect(ui.sqUpgradeLabel('10.0').get()).toBeInTheDocument();
  });
});

function renderEventInner(props: Partial<EventInnerProps> = {}, params?: string) {
  return renderAppWithComponentContext(
    '/',
    () => <Route path="*" element={<EventInner event={mockAnalysisEvent()} {...props} />} />,
    { navigateTo: params ? `/?id=my-project&${params}` : '/?id=my-project' },
  );
}
