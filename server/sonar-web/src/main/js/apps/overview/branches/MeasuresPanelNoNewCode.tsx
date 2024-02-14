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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../components/common/DocLink';
import Link from '../../../components/common/Link';
import { getTabPanelId } from '../../../components/controls/BoxedTabs';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { CodeScope, queryToSearch } from '../../../helpers/urls';
import { Branch } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { NewCodeDefinitionType } from '../../../types/new-code-definition';
import { Component, Period } from '../../../types/types';

export interface MeasuresPanelNoNewCodeProps {
  branch?: Branch;
  component: Component;
  period?: Period;
}

export default function MeasuresPanelNoNewCode(props: MeasuresPanelNoNewCodeProps) {
  const { branch, component, period } = props;

  const isApp = component.qualifier === ComponentQualifier.Application;

  const hasBadReferenceBranch =
    !isApp && !!period && !period.date && period.mode === NewCodeDefinitionType.ReferenceBranch;
  /*
   * If the period is "reference branch"-based, and if there's no date, it means
   * that we're not lacking a second analysis, but that we'll never have new code because the
   * selected reference branch is itself, or has disappeared for some reason.
   * Makes no sense for Apps (project aggregate)
   */
  const hasBadNewCodeSettingSameRef = hasBadReferenceBranch && branch?.name === period?.parameter;

  const badExplanationKey = hasBadReferenceBranch
    ? hasBadNewCodeSettingSameRef
      ? 'overview.measures.same_reference.explanation'
      : 'overview.measures.bad_reference.explanation'
    : 'overview.measures.empty_explanation';

  const showSettingsLink = !!(component.configuration && component.configuration.showSettings);

  return (
    <div
      className="display-flex-center display-flex-justify-center"
      id={getTabPanelId(CodeScope.New)}
      style={{ height: 500 }}
    >
      <img
        alt="" /* Make screen readers ignore this image; it's purely eye candy. */
        className="spacer-right"
        height={52}
        src={`${getBaseUrl()}/images/source-code.svg`}
      />
      <div className="big-spacer-left text-muted" style={{ maxWidth: 500 }}>
        <p className="spacer-bottom big-spacer-top big">{translate(badExplanationKey)}</p>
        {hasBadNewCodeSettingSameRef ? (
          showSettingsLink && (
            <p>
              <FormattedMessage
                defaultMessage={translate('overview.measures.bad_setting.link')}
                id="overview.measures.bad_setting.link"
                values={{
                  setting_link: (
                    <Link
                      to={{
                        pathname: '/project/baseline',
                        search: queryToSearch({ id: component.key, ...getBranchLikeQuery(branch) }),
                      }}
                    >
                      {translate('settings.new_code_period.category')}
                    </Link>
                  ),
                }}
              />
            </p>
          )
        ) : (
          <p>
            <FormattedMessage
              defaultMessage={translate('overview.measures.empty_link')}
              id="overview.measures.empty_link"
              values={{
                learn_more_link: (
                  <DocLink to="/user-guide/clean-as-you-code/">{translate('learn_more')}</DocLink>
                ),
              }}
            />
          </p>
        )}
      </div>
    </div>
  );
}
