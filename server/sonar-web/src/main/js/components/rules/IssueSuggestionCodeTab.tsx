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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { InProgressVisual, OverviewQGNotComputedIcon, OverviewQGPassedIcon } from '~design-system';
import { translate } from '../../helpers/l10n';
import { usePrefetchSuggestion, useUnifiedSuggestionsQuery } from '../../queries/fix-suggestions';
import { useRawSourceQuery } from '../../queries/sources';
import { getBranchLikeQuery } from '../../sonar-aligned/helpers/branch-like';
import { BranchLike } from '../../types/branch-like';
import { Issue } from '../../types/types';
import { IssueSuggestionFileSnippet } from './IssueSuggestionFileSnippet';

interface Props {
  branchLike?: BranchLike;
  issue: Issue;
  language?: string;
}

export function IssueSuggestionCodeTab({ branchLike, issue, language }: Readonly<Props>) {
  const prefetchSuggestion = usePrefetchSuggestion(issue.key);
  const { isPending, isLoading, isError, refetch } = useUnifiedSuggestionsQuery(issue, false);
  const { isError: isIssueRawError } = useRawSourceQuery({
    ...getBranchLikeQuery(branchLike),
    key: issue.component,
  });

  return (
    <>
      {isPending && !isLoading && !isError && (
        <div className="sw-flex sw-flex-col sw-items-center">
          <OverviewQGPassedIcon className="sw-mt-6" />
          <p className="sw-typo-semibold sw-mt-4">
            {translate('issues.code_fix.let_us_suggest_fix')}
          </p>
          <Button
            className="sw-mt-4"
            onClick={() => prefetchSuggestion()}
            variety={ButtonVariety.Primary}
          >
            {translate('issues.code_fix.get_a_fix_suggestion')}
          </Button>
        </div>
      )}
      {isLoading && (
        <div className="sw-flex sw-pt-6 sw-flex-col sw-items-center">
          <InProgressVisual />
          <p className="sw-typo-semibold sw-mt-4">
            {translate('issues.code_fix.fix_is_being_generated')}
          </p>
        </div>
      )}
      {isError && (
        <div className="sw-flex sw-flex-col sw-items-center">
          <OverviewQGNotComputedIcon className="sw-mt-6" />
          <p className="sw-typo-semibold sw-mt-4">
            {translate('issues.code_fix.something_went_wrong')}
          </p>
          <p className="sw-my-4">{translate('issues.code_fix.not_able_to_generate_fix')}</p>
          {translate('issues.code_fix.check_how_to_fix')}
          {!isIssueRawError && (
            <Button className="sw-mt-4" onClick={() => refetch()} variety={ButtonVariety.Primary}>
              {translate('issues.code_fix.get_a_fix_suggestion')}
            </Button>
          )}
        </div>
      )}

      {!isPending && !isError && (
        <IssueSuggestionFileSnippet branchLike={branchLike} issue={issue} language={language} />
      )}
    </>
  );
}
