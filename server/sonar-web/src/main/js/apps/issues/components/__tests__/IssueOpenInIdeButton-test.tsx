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

import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { addGlobalErrorMessage, addGlobalSuccessMessage } from '../../../../helpers/globalMessages';
import {
  openIssue as openSonarLintIssue,
  probeSonarLintServers,
} from '../../../../helpers/sonarlint';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { IssueOpenInIdeButton, Props } from '../IssueOpenInIdeButton';

jest.mock('../../../../helpers/sonarlint', () => ({
  openIssue: jest.fn().mockResolvedValue(undefined),
  probeSonarLintServers: jest.fn(),
}));

jest.mock('../../../../helpers/globalMessages', () => ({
  addGlobalErrorMessage: jest.fn(),
  addGlobalSuccessMessage: jest.fn(),
}));

const MOCK_IDES = [
  { description: 'IDE description', ideName: 'Some IDE', port: 1234 },
  { description: '', ideName: 'Some other IDE', port: 42000 },
];
const MOCK_ISSUE_KEY = 'issue-key';
const MOCK_PROJECT_KEY = 'project-key';

beforeEach(() => {
  jest.clearAllMocks();
});

it('renders properly', () => {
  renderComponentIssueOpenInIdeButton();

  expect(screen.getByText('open_in_ide')).toBeInTheDocument();

  expect(addGlobalErrorMessage).not.toHaveBeenCalled();
  expect(addGlobalSuccessMessage).not.toHaveBeenCalled();
  expect(openSonarLintIssue).not.toHaveBeenCalled();
  expect(probeSonarLintServers).not.toHaveBeenCalled();
});

it('handles button click with no ide found', async () => {
  const user = userEvent.setup();

  renderComponentIssueOpenInIdeButton();

  await act(async () => {
    await user.click(
      screen.getByRole('button', {
        name: 'open_in_ide',
      }),
    );
  });

  expect(probeSonarLintServers).toHaveBeenCalledWith();

  expect(addGlobalErrorMessage).toHaveBeenCalledWith('issues.open_in_ide.failure');

  expect(openSonarLintIssue).not.toHaveBeenCalled();
  expect(addGlobalSuccessMessage).not.toHaveBeenCalled();
});

it('handles button click with one ide found', async () => {
  const user = userEvent.setup();

  jest.mocked(probeSonarLintServers).mockResolvedValueOnce([MOCK_IDES[0]]);

  renderComponentIssueOpenInIdeButton();

  await act(async () => {
    await user.click(
      screen.getByRole('button', {
        name: 'open_in_ide',
      }),
    );
  });

  expect(probeSonarLintServers).toHaveBeenCalledWith();

  expect(openSonarLintIssue).toHaveBeenCalledWith(
    MOCK_IDES[0].port,
    MOCK_PROJECT_KEY,
    MOCK_ISSUE_KEY,
    undefined,
  );

  expect(addGlobalSuccessMessage).toHaveBeenCalledWith('issues.open_in_ide.success');

  expect(addGlobalErrorMessage).not.toHaveBeenCalled();
});

it('handles button click with several ides found', async () => {
  const user = userEvent.setup();

  jest.mocked(probeSonarLintServers).mockResolvedValueOnce(MOCK_IDES);

  renderComponentIssueOpenInIdeButton();

  await act(async () => {
    await user.click(
      screen.getByRole('button', {
        name: 'open_in_ide',
      }),
    );
  });

  expect(probeSonarLintServers).toHaveBeenCalledWith();

  expect(openSonarLintIssue).not.toHaveBeenCalled();
  expect(addGlobalSuccessMessage).not.toHaveBeenCalled();
  expect(addGlobalErrorMessage).not.toHaveBeenCalled();

  expect(
    screen.getByRole('menuitem', { name: `${MOCK_IDES[0].ideName} - ${MOCK_IDES[0].description}` }),
  ).toBeInTheDocument();

  const secondIde = screen.getByRole('menuitem', { name: MOCK_IDES[1].ideName });

  expect(secondIde).toBeInTheDocument();

  await act(async () => {
    await user.click(secondIde);
  });

  expect(openSonarLintIssue).toHaveBeenCalledWith(
    MOCK_IDES[1].port,
    MOCK_PROJECT_KEY,
    MOCK_ISSUE_KEY,
    undefined,
  );

  expect(addGlobalSuccessMessage).toHaveBeenCalledWith('issues.open_in_ide.success');

  expect(addGlobalErrorMessage).not.toHaveBeenCalled();
});

function renderComponentIssueOpenInIdeButton(props: Partial<Props> = {}) {
  return renderComponent(
    <IssueOpenInIdeButton issueKey={MOCK_ISSUE_KEY} projectKey={MOCK_PROJECT_KEY} {...props} />,
  );
}
