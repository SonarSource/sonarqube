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
import classNames from 'classnames';
import * as React from 'react';
import { Link } from 'react-router-dom';
import TabViewer from '../../../components/rules/TabViewer';
import { getRuleUrl } from '../../../helpers/urls';
import { Component, Issue, RuleDetails } from '../../../types/types';

interface IssueViewerTabsProps {
  component?: Component;
  issue: Issue;
  codeTabContent: React.ReactNode;
  ruleDetails: RuleDetails;
}

export default function IssueViewerTabs(props: IssueViewerTabsProps) {
  const {
    ruleDetails,
    codeTabContent,
    issue: { ruleDescriptionContextKey }
  } = props;
  const {
    component,
    ruleDetails: { name, key },
    issue: { message }
  } = props;
  return (
    <>
      <div
        className={classNames('issue-header', {
          'issue-project-level': component !== undefined
        })}>
        <h1 className="text-bold">{message}</h1>
        <div className="spacer-top big-spacer-bottom">
          <span className="note padded-right">{name}</span>
          <Link className="small" to={getRuleUrl(key)} target="_blank">
            {key}
          </Link>
        </div>
      </div>
      <TabViewer
        ruleDetails={ruleDetails}
        extendedDescription={ruleDetails.htmlNote}
        ruleDescriptionContextKey={ruleDescriptionContextKey}
        codeTabContent={codeTabContent}
        pageType="issues"
      />
    </>
  );
}
