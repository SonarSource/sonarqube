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

import { Link } from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import { HelperHintIcon } from '~design-system';
import DocHelpTooltip from '~sonar-aligned/components/controls/DocHelpTooltip';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { DocLink } from '../../../../../helpers/doc-links';
import { translate } from '../../../../../helpers/l10n';
import { getApplicationAdminUrl } from '../../../../../helpers/urls';
import { useProjectBindingQuery } from '../../../../../queries/devops-integration';
import { AlmKeys } from '../../../../../types/alm-settings';
import { Component } from '../../../../../types/types';

interface Props {
  branchSupportEnabled: boolean;
  canAdminComponent?: boolean;
  component: Component;
  hasManyBranches: boolean;
  isApplication: boolean;
}

export default function BranchHelpTooltip({
  component,
  isApplication,
  hasManyBranches,
  canAdminComponent,
  branchSupportEnabled,
}: Props) {
  const helpIcon = <HelperHintIcon aria-label="help-tooltip" />;
  const { data: projectBinding } = useProjectBindingQuery(component.key);
  const isGitLab = projectBinding != null && projectBinding.alm === AlmKeys.GitLab;

  const intl = useIntl();

  if (isApplication) {
    if (!hasManyBranches && canAdminComponent) {
      return (
        <HelpTooltip
          overlay={
            <>
              <p>{translate('application.branches.help')}</p>
              <hr className="sw-my-2" />
              <Link to={getApplicationAdminUrl(component.key)}>
                {translate('application.branches.link')}
              </Link>
            </>
          }
        >
          {helpIcon}
        </HelpTooltip>
      );
    }
  } else {
    if (!branchSupportEnabled) {
      return (
        <DocHelpTooltip
          content={
            projectBinding != null
              ? intl.formatMessage(
                  {
                    id: `branch_like_navigation.no_branch_support.content_x.${isGitLab ? 'mr' : 'pr'}`,
                  },
                  { alm: translate('alm', projectBinding.alm) },
                )
              : translate('branch_like_navigation.no_branch_support.content')
          }
          data-test="branches-support-disabled"
          links={[
            {
              href: 'https://www.sonarsource.com/plans-and-pricing/developer/',
              label: translate('learn_more'),
              doc: false,
            },
          ]}
          title={
            projectBinding != null
              ? translate('branch_like_navigation.no_branch_support.title', isGitLab ? 'mr' : 'pr')
              : translate('branch_like_navigation.no_branch_support.title')
          }
        >
          {helpIcon}
        </DocHelpTooltip>
      );
    }

    if (!hasManyBranches) {
      return (
        <DocHelpTooltip
          content={translate('branch_like_navigation.only_one_branch.content')}
          data-test="only-one-branch-like"
          links={[
            {
              href: DocLink.BranchAnalysis,
              label: translate('branch_like_navigation.only_one_branch.documentation'),
            },
            {
              href: DocLink.PullRequestAnalysis,
              label: translate('branch_like_navigation.only_one_branch.pr_analysis'),
            },
            {
              doc: false,
              href: `/tutorials?id=${component.key}`,
              inPlace: true,
              label: translate('branch_like_navigation.tutorial_for_ci'),
            },
          ]}
          title={translate('branch_like_navigation.only_one_branch.title')}
        >
          {helpIcon}
        </DocHelpTooltip>
      );
    }
  }

  return null;
}
