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
import { Note, SeparatorCircleIcon, TextSubdued } from 'design-system';
import * as React from 'react';
import DocHelpTooltip from '~sonar-aligned/components/controls/DocHelpTooltip';
import IssueSeverityIcon from '../../../components/icon-mappers/IssueSeverityIcon';
import IssueTypeIcon from '../../../components/icon-mappers/IssueTypeIcon';
import TagsList from '../../../components/tags/TagsList';
import { translate } from '../../../helpers/l10n';
import { IssueSeverity } from '../../../types/issues';
import { Dict, RuleDetails } from '../../../types/types';
import RuleDetailsTagsPopup from './RuleDetailsTagsPopup';

interface Props {
  canWrite: boolean | undefined;
  onTagsChange: (tags: string[]) => void;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  ruleDetails: RuleDetails;
}

export default function RuleDetailsHeaderActions(props: Readonly<Props>) {
  const { canWrite, ruleDetails, onTagsChange } = props;
  const { sysTags = [], tags = [] } = ruleDetails;
  const allTags = [...sysTags, ...tags];
  const TAGS_TO_DISPLAY = 1;

  return (
    <Note className="sw-flex sw-flex-wrap sw-items-center sw-gap-2 sw-body-xs">
      {/* Type */}
      <DocHelpTooltip
        content={
          <>
            <p className="sw-mb-4">{translate('coding_rules.type.deprecation.title')}</p>
            <p>{translate('coding_rules.type.deprecation.filter_by')}</p>
          </>
        }
        links={[
          {
            href: '/user-guide/rules/overview',
            label: translate('learn_more'),
          },
        ]}
      >
        <TextSubdued
          className="it__coding-rules-detail-property sw-flex sw-items-center sw-gap-1"
          data-meta="type"
        >
          <IssueTypeIcon
            fill="iconTypeDisabled"
            type={ruleDetails.type}
            aria-hidden
            width={12}
            height={12}
          />
          {translate('issue.type', ruleDetails.type)}
        </TextSubdued>
      </DocHelpTooltip>
      <SeparatorCircleIcon />

      {/* Severity */}
      <DocHelpTooltip
        content={
          <>
            <p className="sw-mb-4">{translate('coding_rules.severity.deprecation.title')}</p>
            <p>{translate('coding_rules.severity.deprecation.filter_by')}</p>
          </>
        }
        links={[
          {
            href: '/user-guide/rules/overview',
            label: translate('learn_more'),
          },
        ]}
      >
        <TextSubdued
          className="it__coding-rules-detail-property sw-flex sw-items-center sw-gap-1"
          data-meta="severity"
        >
          <IssueSeverityIcon
            fill="iconSeverityDisabled"
            severity={ruleDetails.severity as IssueSeverity}
            aria-hidden
            width={12}
            height={12}
          />
          {translate('severity', ruleDetails.severity)}
        </TextSubdued>
      </DocHelpTooltip>
      <SeparatorCircleIcon />

      {/* Tags */}
      <div className="it__coding-rules-detail-property" data-meta="tags">
        <TagsList
          className="sw-body-xs"
          tagsClassName="sw-body-xs"
          allowUpdate={canWrite}
          tagsToDisplay={TAGS_TO_DISPLAY}
          tags={allTags.length > 0 ? allTags : [translate('coding_rules.no_tags')]}
          overlay={
            canWrite ? (
              <RuleDetailsTagsPopup setTags={onTagsChange} sysTags={sysTags} tags={tags} />
            ) : undefined
          }
        />
      </div>
    </Note>
  );
}
