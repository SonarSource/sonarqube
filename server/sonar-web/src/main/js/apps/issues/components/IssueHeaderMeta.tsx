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
import styled from '@emotion/styled';
import { BasicSeparator, LightLabel, themeBorder, Tooltip } from 'design-system';
import React from 'react';
import DateFromNow from '../../../components/intl/DateFromNow';
import IssueTags from '../../../components/issue/components/IssueTags';
import { translate } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';

interface Props {
  issue: Issue;
  canSetTags: boolean;
  onIssueChange: (issue: Issue) => void;
  tagsPopupOpen?: boolean;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default function IssueHeaderMeta(props: Props) {
  const { issue, canSetTags, tagsPopupOpen } = props;

  const separator = <BasicSeparator className="sw-my-2" />;

  return (
    <StyledSection className="sw-flex sw-flex-col sw-pl-4 sw-min-w-abs-150 sw-max-w-abs-250">
      <HotspotHeaderInfo title={translate('issue.tags')}>
        <IssueTags
          canSetTags={canSetTags}
          issue={issue}
          onChange={props.onIssueChange}
          open={tagsPopupOpen}
          togglePopup={props.togglePopup}
          tagsToDisplay={1}
        />
      </HotspotHeaderInfo>
      {separator}

      {!!issue.codeVariants?.length && (
        <>
          <HotspotHeaderInfo title={translate('issue.code_variants')} className="sw-truncate">
            <Tooltip overlay={issue.codeVariants.join(', ')}>
              <span>{issue.codeVariants.join(', ')}</span>
            </Tooltip>
          </HotspotHeaderInfo>
          {separator}
        </>
      )}

      {issue.effort && (
        <>
          <HotspotHeaderInfo title={translate('issue.effort')}>{issue.effort}</HotspotHeaderInfo>
          {separator}
        </>
      )}

      <HotspotHeaderInfo title={translate('issue.introduced')}>
        <DateFromNow date={issue.creationDate} />
      </HotspotHeaderInfo>
    </StyledSection>
  );
}

interface IssueHeaderMetaItemProps {
  children: React.ReactNode;
  title: string;
  className?: string;
}

function HotspotHeaderInfo({ children, title, className }: IssueHeaderMetaItemProps) {
  return (
    <div className={className}>
      <LightLabel as="div" className="sw-body-sm-highlight">
        {title}
      </LightLabel>
      {children}
    </div>
  );
}

const StyledSection = styled.div`
  border-left: ${themeBorder('default', 'pageBlockBorder')};
`;
