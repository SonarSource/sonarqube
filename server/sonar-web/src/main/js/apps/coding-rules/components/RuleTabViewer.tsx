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
import { sanitizeString } from '../../../helpers/sanitize';
import { RuleDetails } from '../../../types/types';
import { RuleDescriptionSections } from '../rule';

export interface RuleTabViewerProps {
  ruleDetails: RuleDetails;
}

export default function RuleTabViewer(props: RuleTabViewerProps) {
  const { ruleDetails } = props;
  const introduction = ruleDetails.descriptionSections?.find(
    section => section.key === RuleDescriptionSections.INTRODUCTION
  )?.content;

  return (
    <>
      {introduction && (
        <div
          className="rule-desc"
          // eslint-disable-next-line react/no-danger
          dangerouslySetInnerHTML={{ __html: sanitizeString(introduction) }}
        />
      )}
      <TabViewer ruleDetails={ruleDetails} />
    </>
  );
}
