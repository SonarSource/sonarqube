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

import { useMutation } from '@tanstack/react-query';
import { addGlobalErrorMessage, addGlobalSuccessMessage } from '~design-system';
import { useCurrentUser } from '../app/components/current-user/CurrentUserContext';
import { translate } from '../helpers/l10n';
import { generateSonarLintUserToken, openFixOrIssueInSonarLint } from '../helpers/sonarlint';
import { BranchLike } from '../types/branch-like';
import { Fix, Ide } from '../types/sonarlint';
import { Issue } from '../types/types';
import { isLoggedIn } from '../types/users';

export function useOpenFixOrIssueInIdeMutation() {
  const { currentUser } = useCurrentUser();
  const login: string | undefined = isLoggedIn(currentUser) ? currentUser.login : undefined;

  return useMutation({
    mutationFn: async (data: {
      branchLike: BranchLike | undefined;
      fix?: Fix;
      ide: Ide;
      issue: Issue;
    }) => {
      const { ide, branchLike, issue, fix } = data;

      const { key: issueKey, projectKey } = issue;

      let token;
      if (ide.needsToken && login !== undefined) {
        token = await generateSonarLintUserToken({ ideName: ide.ideName, login });
      }

      return openFixOrIssueInSonarLint({
        branchLike,
        calledPort: ide.port,
        issueKey,
        projectKey,
        token,
        fix,
      });
    },
    onSuccess: (_, arg) => {
      if (arg.fix) {
        addGlobalSuccessMessage(translate('fix_in_ide.report_success'));
      } else {
        addGlobalSuccessMessage(translate('open_in_ide.report_success'));
      }
    },
    onError: (_, arg) => {
      if (arg.fix) {
        addGlobalErrorMessage(translate('fix_in_ide.report_error'));
      } else {
        addGlobalErrorMessage(translate('open_in_ide.report_error'));
      }
    },
  });
}
