/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ProjectBitbucketBindingResponse } from '../../../types/alm-settings';
import LabelActionPair from '../components/LabelActionPair';
import LabelValuePair from '../components/LabelValuePair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import Step from '../components/Step';

export interface MultiBranchPipelineStepProps {
  finished: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
  projectBinding: ProjectBitbucketBindingResponse;
}

export default function MultiBranchPipelineStep(props: MultiBranchPipelineStepProps) {
  const { finished, open, projectBinding } = props;
  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={() => (
        <div className="boxed-group-inner">
          <p className="big-spacer-bottom">
            {translate('onboarding.tutorial.with.jenkins.multi_branch_pipeline.intro')}
          </p>
          <ol className="list-styled">
            <li>
              <SentenceWithHighlights
                highlightKeys={['new_item', 'type']}
                translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step1"
              />
            </li>
            <li>
              <SentenceWithHighlights
                highlightKeys={['tab']}
                translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2"
              />
              <ul className="list-styled">
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.server" />
                </li>
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.creds" />
                </li>
                <li>
                  <LabelValuePair
                    translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.owner"
                    value={projectBinding.repository}
                  />
                </li>
                <li>
                  <LabelValuePair
                    translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.repo"
                    value={projectBinding.slug}
                  />
                </li>
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviour" />
                </li>
              </ul>
            </li>
            <li>
              <SentenceWithHighlights
                highlightKeys={['tab']}
                translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step3"
              />
              <ul className="list-styled">
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step3.mode" />
                </li>
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step3.script_path" />
                </li>
              </ul>
            </li>
            <li>
              <SentenceWithHighlights
                highlightKeys={['save']}
                translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step4"
              />
            </li>
          </ol>
          <Button onClick={props.onDone}>{translate('continue')}</Button>
        </div>
      )}
      stepNumber={1}
      stepTitle={translate('onboarding.tutorial.with.jenkins.multi_branch_pipeline.title')}
    />
  );
}
