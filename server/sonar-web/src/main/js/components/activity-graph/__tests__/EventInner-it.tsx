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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byRole, byText } from 'testing-library-selector';
import { mockAnalysisEvent } from '../../../helpers/mocks/project-activity';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import EventInner, { EventInnerProps } from '../EventInner';

const ui = {
  showMoreBtn: byRole('button', { name: 'more' }),
  showLessBtn: byRole('button', { name: 'hide' }),
  projectLink: (name: string) => byRole('link', { name }),

  definitionChangeLabel: byText('event.category.DEFINITION_CHANGE', { exact: false }),
  projectAddedTxt: byText('event.definition_change.added'),
  projectRemovedTxt: byText('event.definition_change.removed'),
  branchReplacedTxt: byText('event.definition_change.branch_replaced'),

  qualityGateLabel: byText('event.category.QUALITY_GATE', { exact: false }),
  stillFailingTxt: byText('event.quality_gate.still_x'),

  versionLabel: byText('event.category.VERSION', { exact: false }),
};

describe('DEFINITION_CHANGE events', () => {
  it('should render correctly for "DEFINITION_CHANGE" events', async () => {
    const user = userEvent.setup();
    renderEventInner({
      event: mockAnalysisEvent({
        category: 'DEFINITION_CHANGE',
        definitionChange: {
          projects: [
            { changeType: 'ADDED', key: 'foo', name: 'Foo', branch: 'master-foo' },
            { changeType: 'REMOVED', key: 'bar', name: 'Bar', branch: 'master-bar' },
            {
              changeType: 'BRANCH_CHANGED',
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

    // ADDED.
    expect(ui.projectAddedTxt.get()).toBeInTheDocument();
    expect(ui.projectLink('Foo').get()).toBeInTheDocument();
    expect(screen.getByText('master-foo')).toBeInTheDocument();

    // REMOVED.
    expect(ui.projectRemovedTxt.get()).toBeInTheDocument();
    expect(ui.projectLink('Bar').get()).toBeInTheDocument();
    expect(screen.getByText('master-bar')).toBeInTheDocument();

    // BRANCH_CHANGED
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
        category: 'QUALITY_GATE',
        qualityGate: { status: 'ERROR', stillFailing: false, failing: [] },
      }),
    });

    expect(ui.qualityGateLabel.get()).toBeInTheDocument();
  });

  it('should render correctly for "still failing" "QUALITY_GATE" events', () => {
    renderEventInner({
      event: mockAnalysisEvent({
        category: 'QUALITY_GATE',
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
        category: 'QUALITY_GATE',
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
        category: 'VERSION',
        name: '1.0',
      }),
    });

    expect(ui.versionLabel.get()).toBeInTheDocument();
    expect(screen.getByText('1.0')).toBeInTheDocument();
  });
});

function renderEventInner(props: Partial<EventInnerProps> = {}) {
  return renderComponent(<EventInner event={mockAnalysisEvent()} {...props} />);
}
