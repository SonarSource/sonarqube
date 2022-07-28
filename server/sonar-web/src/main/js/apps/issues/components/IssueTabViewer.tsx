/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import TabViewer from '../../../components/rules/TabViewer';
import { BranchLike } from '../../../types/branch-like';
import { Issue, RuleDetails } from '../../../types/types';
import IssueHeader from './IssueHeader';

interface IssueViewerTabsProps {
  branchLike?: BranchLike;
  issue: Issue;
  codeTabContent: React.ReactNode;
  ruleDetails: RuleDetails;
  onIssueChange: (issue: Issue) => void;
}

export default function IssueViewerTabs(props: IssueViewerTabsProps) {
  const { ruleDetails, issue, codeTabContent, branchLike } = props;
  return (
    <>
      <IssueHeader
        issue={issue}
        ruleDetails={ruleDetails}
        branchLike={branchLike}
        onIssueChange={props.onIssueChange}
      />
      <TabViewer
        ruleDetails={ruleDetails}
        extendedDescription={ruleDetails.htmlNote}
        ruleDescriptionContextKey={issue.ruleDescriptionContextKey}
        codeTabContent={codeTabContent}
        scrollInTab={true}
      />
    </>
  );
}
