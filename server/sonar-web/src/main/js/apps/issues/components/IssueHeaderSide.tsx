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
import { LightLabel, themeBorder } from 'design-system';
import React from 'react';
import { CleanCodeAttributePill } from '../../../components/shared/CleanCodeAttributePill';
import SoftwareImpactPillList from '../../../components/shared/SoftwareImpactPillList';
import { translate } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';

interface Props {
  issue: Issue;
}

export default function IssueHeaderSide({ issue }: Readonly<Props>) {
  return (
    <StyledSection className="sw-flex sw-flex-col sw-pl-4 sw-max-w-[250px]">
      <IssueHeaderInfo title={translate('issue.cct_attribute.label')} className="sw-mb-6">
        <CleanCodeAttributePill
          cleanCodeAttributeCategory={issue.cleanCodeAttributeCategory}
          cleanCodeAttribute={issue.cleanCodeAttribute}
        />
      </IssueHeaderInfo>

      <IssueHeaderInfo
        data-guiding-id="issue-2"
        title={translate('issue.software_qualities.label')}
      >
        <SoftwareImpactPillList className="sw-flex-wrap" softwareImpacts={issue.impacts} />
      </IssueHeaderInfo>
    </StyledSection>
  );
}

interface IssueHeaderMetaItemProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  title: string;
  className?: string;
}

function IssueHeaderInfo({
  children,
  title,
  className,
  ...props
}: Readonly<IssueHeaderMetaItemProps>) {
  return (
    <div className={className} {...props}>
      <LightLabel as="div" className="sw-text-xs sw-font-semibold sw-mb-1">
        {title}
      </LightLabel>
      {children}
    </div>
  );
}

const StyledSection = styled.div`
  border-left: ${themeBorder('default', 'pageBlockBorder')};
`;
