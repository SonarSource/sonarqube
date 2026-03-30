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

import { without } from 'lodash';
import { useIntl } from 'react-intl';
import { FacetBox, FacetItem } from '~design-system';
import { IssueCodefixStatus } from '../../../types/issues';
import { formatFacetStat } from '../utils';
import { FacetItemsList } from './FacetItemsList';
import { CommonProps } from './SimpleListStyleFacet';

const PROPERTY = 'issueCodefixStatuses';
const HEADER_ID = `facet_${PROPERTY}`;

export const AI_CODEFIX_STATUSES: IssueCodefixStatus[] = [
  IssueCodefixStatus.AiFixAvailable,
  IssueCodefixStatus.AiFixInProgress,
  IssueCodefixStatus.AiFixGenerated,
  IssueCodefixStatus.PullRequestCreated,
  IssueCodefixStatus.AiFixFailed,
];

const STATUS_LABEL_IDS: Record<IssueCodefixStatus, string> = {
  [IssueCodefixStatus.AiFixAvailable]: 'issues.facet.ai_code_assistant.ai_fix_available',
  [IssueCodefixStatus.AiFixGenerated]: 'issues.facet.ai_code_assistant.ai_fix_generated',
  [IssueCodefixStatus.AiFixFailed]: 'issues.facet.ai_code_assistant.ai_fix_failed',
  [IssueCodefixStatus.AiFixInProgress]: 'issues.facet.ai_code_assistant.ai_fix_in_progress',
  [IssueCodefixStatus.PullRequestCreated]: 'issues.facet.ai_code_assistant.pull_request_created',
};

interface Props extends CommonProps {
  issueCodefixStatuses: IssueCodefixStatus[];
}

export function AiCodeAssistantFacet(props: Readonly<Props>) {
  const { issueCodefixStatuses = [], stats = {}, fetching, open, needIssueSync } = props;
  const intl = useIntl();

  const getStatusLabel = (status: IssueCodefixStatus) => {
    const id = STATUS_LABEL_IDS[status];
    const defaultMessages: Record<IssueCodefixStatus, string> = {
      [IssueCodefixStatus.AiFixAvailable]: 'AI Fix Available',
      [IssueCodefixStatus.AiFixGenerated]: 'AI Fix Generated',
      [IssueCodefixStatus.AiFixFailed]: 'AI Fix Failed',
      [IssueCodefixStatus.AiFixInProgress]: 'AI Fix in Progress',
      [IssueCodefixStatus.PullRequestCreated]: 'Pull Request Created',
    };
    const defaultMessage = defaultMessages[status];
    const msg = intl.formatMessage({ id, defaultMessage });
    return msg !== id ? msg : defaultMessage;
  };

  return (
    <FacetBox
      className="it__search-navigator-facet-box it__search-navigator-facet-header"
      count={issueCodefixStatuses.length}
      countLabel={intl.formatMessage(
        { id: 'x_selected' },
        { '0': issueCodefixStatuses.length },
      )}
      data-property={PROPERTY}
      id={HEADER_ID}
      loading={fetching}
      name={intl.formatMessage({
        id: 'issues.facet.ai_code_assistant',
        defaultMessage: 'AI Code Assistant',
      })}
      onClear={() => props.onChange({ [PROPERTY]: [] })}
      onClick={() => props.onToggle(PROPERTY)}
      open={open}
    >
      <FacetItemsList labelledby={HEADER_ID}>
        {AI_CODEFIX_STATUSES.map((item) => {
          const active = issueCodefixStatuses.includes(item);
          const stat = stats[item];

          return (
            <FacetItem
              active={active}
              className="it__search-navigator-facet"
              key={item}
              name={getStatusLabel(item)}
              onClick={(itemValue: IssueCodefixStatus, multiple) => {
                if (multiple) {
                  props.onChange({
                    [PROPERTY]: active
                      ? without(issueCodefixStatuses, itemValue)
                      : [...issueCodefixStatuses, itemValue],
                  });
                } else {
                  props.onChange({
                    [PROPERTY]: active && issueCodefixStatuses.length === 1 ? [] : [itemValue],
                  });
                }
              }}
              stat={(!needIssueSync && formatFacetStat(stat)) ?? 0}
              value={item}
            />
          );
        })}
      </FacetItemsList>
    </FacetBox>
  );
}
