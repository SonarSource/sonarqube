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
import { addGlobalErrorMessage, addGlobalSuccessMessage } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { DocLink } from '../../../../helpers/doc-links';
import {
  openIssue as openSonarLintIssue,
  probeSonarLintServers,
} from '../../../../helpers/sonarlint';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { Ide } from '../../../../types/sonarlint';
import { IssueOpenInIdeButton, Props } from '../IssueOpenInIdeButton';

jest.mock('../../../../helpers/sonarlint', () => ({
  generateSonarLintUserToken: jest
    .fn()
    .mockResolvedValue({ name: 'token name', token: 'token value' }),
  openIssue: jest.fn().mockResolvedValue(undefined),
  probeSonarLintServers: jest.fn(),
}));

jest.mock('design-system', () => ({
  ...jest.requireActual('design-system'),
  addGlobalErrorMessage: jest.fn(),
  addGlobalSuccessMessage: jest.fn(),
}));

const MOCK_IDES: Ide[] = [
  { description: 'IDE description', ideName: 'Some IDE', port: 1234 },
  { description: '', ideName: 'Some other IDE', needsToken: true, port: 42000 },
];

const MOCK_ISSUE_KEY = 'issue-key';
const MOCK_PROJECT_KEY = 'project-key';

let tokenMock: UserTokensMock;

beforeAll(() => {
  tokenMock = new UserTokensMock();
});

afterEach(() => {
  tokenMock.reset();
});

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

  await user.click(
    screen.getByRole('button', {
      name: 'open_in_ide',
    }),
  );

  expect(probeSonarLintServers).toHaveBeenCalledWith();

  expect(addGlobalErrorMessage).toHaveBeenCalledWith(
    <FormattedMessage
      id="issues.open_in_ide.failure"
      values={{
        link: (
          <DocumentationLink to={DocLink.SonarLintConnectedMode}>
            sonarlint-connected-mode-doc
          </DocumentationLink>
        ),
      }}
    />,
  );

  expect(openSonarLintIssue).not.toHaveBeenCalled();
  expect(addGlobalSuccessMessage).not.toHaveBeenCalled();
});

it('handles button click with one ide found', async () => {
  const user = userEvent.setup();

  jest.mocked(probeSonarLintServers).mockResolvedValueOnce([MOCK_IDES[0]]);

  renderComponentIssueOpenInIdeButton();

  await user.click(
    screen.getByRole('button', {
      name: 'open_in_ide',
    }),
  );

  expect(probeSonarLintServers).toHaveBeenCalledWith();

  expect(openSonarLintIssue).toHaveBeenCalledWith({
    branchName: undefined,
    calledPort: MOCK_IDES[0].port,
    issueKey: MOCK_ISSUE_KEY,
    projectKey: MOCK_PROJECT_KEY,
    pullRequestID: undefined,
    tokenName: undefined,
    tokenValue: undefined,
  });

  expect(addGlobalSuccessMessage).toHaveBeenCalledWith('issues.open_in_ide.success');

  expect(addGlobalErrorMessage).not.toHaveBeenCalled();
});

it('handles button click with several ides found', async () => {
  const user = userEvent.setup();

  jest.mocked(probeSonarLintServers).mockResolvedValueOnce(MOCK_IDES);

  renderComponentIssueOpenInIdeButton();

  await user.click(
    screen.getByRole('button', {
      name: 'open_in_ide',
    }),
  );

  expect(probeSonarLintServers).toHaveBeenCalledWith();

  expect(openSonarLintIssue).not.toHaveBeenCalled();
  expect(addGlobalSuccessMessage).not.toHaveBeenCalled();
  expect(addGlobalErrorMessage).not.toHaveBeenCalled();

  expect(
    screen.getByRole('menuitem', { name: `${MOCK_IDES[0].ideName} - ${MOCK_IDES[0].description}` }),
  ).toBeInTheDocument();

  const secondIde = screen.getByRole('menuitem', { name: MOCK_IDES[1].ideName });

  expect(secondIde).toBeInTheDocument();

  await user.click(secondIde);

  expect(openSonarLintIssue).toHaveBeenCalledWith({
    branchName: undefined,
    calledPort: MOCK_IDES[1].port,
    issueKey: MOCK_ISSUE_KEY,
    projectKey: MOCK_PROJECT_KEY,
    pullRequestID: undefined,
    tokenName: 'token name',
    tokenValue: 'token value',
  });

  expect(addGlobalSuccessMessage).toHaveBeenCalledWith('issues.open_in_ide.success');

  expect(addGlobalErrorMessage).not.toHaveBeenCalled();
});

function renderComponentIssueOpenInIdeButton(props: Partial<Props> = {}) {
  return renderComponent(
    <IssueOpenInIdeButton
      issueKey={MOCK_ISSUE_KEY}
      login="login-1"
      projectKey={MOCK_PROJECT_KEY}
      {...props}
    />,
  );
}
