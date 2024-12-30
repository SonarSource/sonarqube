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

import { useIntl } from 'react-intl';
import { DiscreetLink, LightPrimary, PullRequestIcon, SubTitle } from '~design-system';
import { getPullRequestUrl } from '../../../helpers/urls';
import { PullRequest } from '../../../types/branch-like';
import { Component } from '../../../types/types';

interface IssuesListTitleProps {
  component?: Component;
  fixedInPullRequest: string;
  pullRequests: PullRequest[];
}

export default function IssuesListTitle({
  fixedInPullRequest,
  pullRequests,
  component,
}: Readonly<IssuesListTitleProps>) {
  const intl = useIntl();
  const pullRequest = pullRequests.find((pr) => pr.key === fixedInPullRequest);
  const prSummaryUrl = getPullRequestUrl(component?.key ?? '', fixedInPullRequest);

  return pullRequest && !component?.needIssueSync ? (
    <>
      <SubTitle className="sw-mt-6 sw-mb-2">
        {intl.formatMessage({ id: 'issues.fixed_issues' })}
      </SubTitle>
      <LightPrimary className="sw-flex sw-items-center sw-gap-1 sw-mb-2">
        {intl.formatMessage(
          { id: 'issues.fixed_issues.description' },
          {
            pullRequest: (
              <>
                <PullRequestIcon />
                <DiscreetLink to={prSummaryUrl} className="sw-mt-[3px]">
                  {pullRequest.title}
                </DiscreetLink>
              </>
            ),
          },
        )}
      </LightPrimary>
    </>
  ) : (
    <h2 className="sw-sr-only">{intl.formatMessage({ id: 'list_of_issues' })}</h2>
  );
}
