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

import { useQueryClient } from '@tanstack/react-query';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ComponentProps } from 'react';
import BranchesServiceMock from '../../../../api/mocks/BranchesServiceMock';
import { ComponentContext } from '../../../../app/components/componentContext/ComponentContext';
import { mockComponent } from '../../../../helpers/mocks/component';
import { openFixOrIssueInSonarLint, probeSonarLintServers } from '../../../../helpers/sonarlint';
import { mockIssue, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { CodeSuggestion, LineTypeEnum } from '../../../../queries/fix-suggestions';
import { ComponentContextShape } from '../../../../types/component';
import { Fix, Ide } from '../../../../types/sonarlint';
import { OpenFixInIde } from '../OpenFixInIde';

jest.mock('../../../../api/components', () => ({
  getComponentForSourceViewer: jest.fn().mockReturnValue({}),
}));
jest.mock('../../../../helpers/sonarlint', () => ({
  generateSonarLintUserToken: jest
    .fn()
    .mockResolvedValue({ name: 'token name', token: 'token value' }),
  openFixOrIssueInSonarLint: jest.fn().mockResolvedValue(undefined),
  probeSonarLintServers: jest.fn(),
}));

const handler = new BranchesServiceMock();

const MOCK_TOKEN: any = {
  name: 'token name',
  token: 'token value',
};

const FIX_DATA: Fix = {
  explanation: 'explanation',
  fileEdit: {
    changes: [
      {
        after: 'var p = 2;',
        before: 'var t = 1;',
        beforeLineRange: {
          startLine: 1,
          endLine: 2,
        },
      },
    ],
    path: '',
  },
  suggestionId: 'suggestionId',
};

const AI_SUGGESTION: CodeSuggestion = {
  changes: [{ endLine: 2, newCode: 'var p = 2;', startLine: 1 }],
  explanation: 'explanation',
  suggestionId: 'suggestionId',
  unifiedLines: [
    {
      code: 'var t = 1;',
      lineAfter: -1,
      lineBefore: 1,
      type: LineTypeEnum.REMOVED,
    },
    {
      code: 'var p = 2;',
      lineAfter: 1,
      lineBefore: -1,
      type: LineTypeEnum.ADDED,
    },
  ],
};

const MOCK_IDES_OPEN_FIX: Ide[] = [
  {
    description: 'IDE description',
    ideName: 'Some IDE',
    port: 1234,
    capabilities: { canOpenFixSuggestion: true },
    needsToken: false,
  },
  {
    description: '',
    ideName: 'Some other IDE',
    needsToken: true,
    port: 42000,
    capabilities: { canOpenFixSuggestion: true },
  },
  { description: '', ideName: 'Some other IDE 2', needsToken: true, port: 43000 },
];
const MOCK_ISSUE_KEY = 'issue-key';
const MOCK_PROJECT_KEY = 'project-key';

beforeEach(() => {
  handler.reset();
});

it('handles open in ide button click with several ides found when there is fix suggestion', async () => {
  const user = userEvent.setup();

  jest.mocked(probeSonarLintServers).mockResolvedValueOnce(MOCK_IDES_OPEN_FIX);

  renderComponentOpenIssueInIdeButton();

  await user.click(
    await screen.findByRole('button', {
      name: 'view_fix_in_ide',
    }),
  );

  expect(
    screen.getByRole('menuitem', {
      name: `${MOCK_IDES_OPEN_FIX[0].ideName} - ${MOCK_IDES_OPEN_FIX[0].description}`,
    }),
  ).toBeInTheDocument();

  const secondIde = screen.getByRole('menuitem', { name: MOCK_IDES_OPEN_FIX[1].ideName });

  expect(secondIde).toBeInTheDocument();

  await user.click(secondIde);

  expect(openFixOrIssueInSonarLint).toHaveBeenCalledWith({
    branchLike: expect.objectContaining({ isMain: true, name: 'main' }),
    calledPort: MOCK_IDES_OPEN_FIX[1].port,
    fix: FIX_DATA,
    issueKey: MOCK_ISSUE_KEY,
    projectKey: MOCK_PROJECT_KEY,
    token: MOCK_TOKEN,
  });
});

function renderComponentOpenIssueInIdeButton(
  props: Partial<ComponentProps<typeof OpenFixInIde>> = {},
) {
  const mockedIssue = mockIssue(false, {
    key: MOCK_ISSUE_KEY,
    projectKey: MOCK_PROJECT_KEY,
  });

  const componentContext: ComponentContextShape = {
    fetchComponent: jest.fn(),
    onComponentChange: jest.fn(),
    component: mockComponent(),
  };

  function Wrapper() {
    const queryClient = useQueryClient();
    queryClient.setQueryData(['branches', 'mycomponent', 'details'], { branchLike: {} });

    return (
      <ComponentContext.Provider value={componentContext}>
        <OpenFixInIde aiSuggestion={AI_SUGGESTION} issue={mockedIssue} {...props} />
      </ComponentContext.Provider>
    );
  }

  return renderComponent(<Wrapper />, '/?id=mycomponent', { currentUser: mockLoggedInUser() });
}
