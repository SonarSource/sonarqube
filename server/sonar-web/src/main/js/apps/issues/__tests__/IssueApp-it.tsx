/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import React from 'react';
import IssuesServiceMock from '../../../api/mocks/IssuesServiceMock';
import { renderOwaspTop102021Category } from '../../../helpers/security-standard';
import { renderApp, renderComponentApp } from '../../../helpers/testReactTestingUtils';
import { IssueType } from '../../../types/issues';
import AppContainer from '../components/AppContainer';
import { projectIssuesRoutes } from '../routes';

jest.mock('../../../api/issues');
jest.mock('../../../api/rules');
jest.mock('../../../api/components');

let handler: IssuesServiceMock;

beforeEach(() => {
  window.scrollTo = jest.fn();
  handler = new IssuesServiceMock();
});

it('should open issue and navigate', async () => {
  const user = userEvent.setup();
  renderIssueApp();
  expect(await screen.findByRole('region', { name: 'Fix that' })).toBeInTheDocument();
  await user.click(screen.getByRole('region', { name: 'Fix that' }));
  expect(screen.getByRole('heading', { level: 1, name: 'Fix that' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'advancedRuleId' })).toBeInTheDocument();

  expect(screen.getByRole('button', { name: `issue.tabs.resources` })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: `issue.tabs.resources` }));
  expect(screen.getByRole('heading', { name: 'Link' })).toBeInTheDocument();

  expect(screen.getByRole('button', { name: `issue.tabs.how` })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: `issue.tabs.how` }));
  expect(screen.getByRole('radio', { name: 'Context 2' })).toBeInTheDocument();
  expect(screen.getByRole('radio', { name: 'Context 3' })).toBeInTheDocument();
  expect(screen.getByRole('radio', { name: 'Spring' })).toBeInTheDocument();
  expect(
    screen.getByRole('radio', { name: 'coding_rules.description_context_other' })
  ).toBeInTheDocument();

  await user.click(screen.getByRole('radio', { name: 'Context 2' }));
  expect(screen.getByText('Context 2 content')).toBeInTheDocument();

  await user.click(screen.getByRole('radio', { name: 'coding_rules.description_context_other' }));
  expect(screen.getByText('coding_rules.context.others.title')).toBeInTheDocument();
  expect(screen.getByText('coding_rules.context.others.description.first')).toBeInTheDocument();
  expect(screen.getByText('coding_rules.context.others.description.second')).toBeInTheDocument();

  expect(screen.getByRole('button', { name: `issue.tabs.why` })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: `issue.tabs.why` }));
  expect(screen.getByRole('heading', { name: 'Because' })).toBeInTheDocument();

  await user.keyboard('{ArrowUp}');

  expect(screen.getByRole('heading', { level: 1, name: 'Fix this' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'simpleRuleId' })).toBeInTheDocument();

  expect(screen.getByRole('button', { name: `issue.tabs.why` })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: `issue.tabs.why` }));
  expect(screen.getByRole('heading', { name: 'Default' })).toBeInTheDocument();

  await user.keyboard('{ArrowUp}');

  expect(screen.getByRole('heading', { level: 1, name: 'Issue on file' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'simpleRuleId' })).toBeInTheDocument();

  expect(screen.getByRole('button', { name: `issue.tabs.code` })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: `issue.tabs.code` }));
  expect(screen.getByRole('region', { name: 'Issue on file' })).toBeInTheDocument();
  expect(
    screen.getByRole('row', {
      name: '2 source_viewer.tooltip.covered import java.util. ArrayList ;'
    })
  ).toBeInTheDocument();
});

it('should be able to navigate to other issue located in the same file', async () => {
  const user = userEvent.setup();
  renderIssueApp();
  await user.click(await screen.findByRole('region', { name: 'Fix that' }));
  expect(await screen.findByRole('region', { name: 'Second issue' })).toBeInTheDocument();
  await user.click(await screen.findByRole('region', { name: 'Second issue' }));
  expect(screen.getByRole('heading', { level: 1, name: 'Second issue' })).toBeInTheDocument();
});

it('should support OWASP Top 10 version 2021', async () => {
  const user = userEvent.setup();
  renderIssueApp();
  await user.click(await screen.findByRole('link', { name: 'issues.facet.standards' }));
  const owaspTop102021 = screen.getByRole('link', { name: 'issues.facet.owaspTop10_2021' });
  expect(owaspTop102021).toBeInTheDocument();

  await user.click(owaspTop102021);
  await Promise.all(
    handler.owasp2021FacetList().values.map(async ({ val }) => {
      const standard = await handler.getStandards();
      /* eslint-disable-next-line testing-library/render-result-naming-convention */
      const linkName = renderOwaspTop102021Category(standard, val);
      expect(await screen.findByRole('link', { name: linkName })).toBeInTheDocument();
    })
  );
});

describe('redirects', () => {
  it('should work for hotspots', () => {
    renderProjectIssuesApp(`project/issues?types=${IssueType.SecurityHotspot}`);

    expect(screen.getByText('/security_hotspots?assignedToMe=false')).toBeInTheDocument();
  });

  it('should filter out hotspots', async () => {
    renderProjectIssuesApp(
      `project/issues?types=${IssueType.SecurityHotspot},${IssueType.CodeSmell}`
    );

    expect(
      await screen.findByRole('link', { name: `issue.type.${IssueType.CodeSmell}` })
    ).toBeInTheDocument();
  });
});

function renderIssueApp() {
  renderComponentApp('project/issues', <AppContainer />);
}

function renderProjectIssuesApp(navigateTo?: string) {
  renderApp('project/issues', projectIssuesRoutes, { navigateTo });
}
