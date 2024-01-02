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
import { rawSizes } from '../../../app/theme';
import { Button } from '../../../components/controls/buttons';
import ChevronRightIcon from '../../../components/icons/ChevronRightIcon';
import { translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import LabelActionPair from '../components/LabelActionPair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import Step from '../components/Step';

export interface PipelineStepProps {
  alm: AlmKeys;
  finished: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
}

export default function PipelineStep(props: PipelineStepProps) {
  const { alm, finished, open } = props;
  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={() => (
        <div className="boxed-group-inner">
          <p className="big-spacer-bottom">
            {translate('onboarding.tutorial.with.jenkins.pipeline.intro')}
          </p>
          <ol className="list-styled">
            <li>
              <SentenceWithHighlights
                highlightKeys={['new_item', 'type']}
                translationKey="onboarding.tutorial.with.jenkins.pipeline.step1"
              />
            </li>
            <li>
              {alm === AlmKeys.GitLab ? (
                <>
                  <SentenceWithHighlights
                    highlightKeys={['tab', 'option']}
                    translationKey="onboarding.tutorial.with.jenkins.pipeline.gitlab.step2.trigger"
                  />
                  <ul className="list-styled">
                    <li>
                      <SentenceWithHighlights
                        highlightKeys={['triggers', 'push_events']}
                        translationKey="onboarding.tutorial.with.jenkins.pipeline.gitlab.step2.pick_triggers"
                      />
                    </li>
                    <li>
                      <SentenceWithHighlights
                        highlightKeys={['advanced']}
                        translationKey="onboarding.tutorial.with.jenkins.pipeline.gitlab.step2.click_advanced"
                      />
                    </li>
                    <li>
                      <SentenceWithHighlights
                        highlightKeys={['secret_token', 'generate']}
                        translationKey="onboarding.tutorial.with.jenkins.pipeline.gitlab.step2.secret_token"
                      />
                    </li>
                  </ul>
                </>
              ) : (
                <SentenceWithHighlights
                  highlightKeys={['tab', 'option']}
                  translationKey="onboarding.tutorial.with.jenkins.pipeline.step2"
                />
              )}
            </li>
            <li>
              <SentenceWithHighlights
                highlightKeys={['tab']}
                translationKey="onboarding.tutorial.with.jenkins.pipeline.step3"
              />
              <ul className="list-styled">
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.pipeline.step3.definition" />
                </li>
                <li>
                  <SentenceWithHighlights
                    highlightKeys={['label', 'branches_to_build']}
                    translationKey="onboarding.tutorial.with.jenkins.pipeline.step3.scm"
                  />
                </li>
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.pipeline.step3.script_path" />
                </li>
              </ul>
            </li>
            <li>
              <SentenceWithHighlights
                highlightKeys={['save']}
                translationKey="onboarding.tutorial.with.jenkins.pipeline.step4"
              />
            </li>
          </ol>
          <Button className="big-spacer-top" onClick={props.onDone}>
            {translate('continue')}
            <ChevronRightIcon size={rawSizes.baseFontSizeRaw} />
          </Button>
        </div>
      )}
      stepNumber={1}
      stepTitle={translate('onboarding.tutorial.with.jenkins.pipeline.title')}
    />
  );
}
