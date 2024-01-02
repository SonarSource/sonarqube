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
import { NumberedList, NumberedListItem } from 'design-system';
import * as React from 'react';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';
import { BuildTools } from '../../types';
import AlertClassicEditor from './AlertClassicEditor';
import PrepareAnalysisCommand, { PrepareType } from './PrepareAnalysisCommand';
import PublishSteps from './PublishSteps';

export interface OtherProps {
  projectKey: string;
}

export default function Other(props: OtherProps) {
  const { projectKey } = props;
  return (
    <>
      <AlertClassicEditor />
      <NumberedList className="sw-mt-4">
        <NumberedListItem>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare"
            highlightKeys={['pipeline', 'task', 'before']}
          />
          <PrepareAnalysisCommand
            buildTool={BuildTools.Other}
            kind={PrepareType.StandAlone}
            projectKey={projectKey}
          />
        </NumberedListItem>

        <NumberedListItem>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run"
            highlightKeys={['task', 'after']}
          />
        </NumberedListItem>

        <PublishSteps />
      </NumberedList>
    </>
  );
}
