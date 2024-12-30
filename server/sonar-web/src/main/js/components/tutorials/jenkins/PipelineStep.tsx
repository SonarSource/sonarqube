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

import {
  ListItem,
  NumberedList,
  NumberedListItem,
  TutorialStep,
  UnorderedList,
} from '~design-system';
import { translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import LabelActionPair from '../components/LabelActionPair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';

export interface PipelineStepProps {
  alm: AlmKeys;
}

export default function PipelineStep(props: PipelineStepProps) {
  const { alm } = props;
  return (
    <TutorialStep title={translate('onboarding.tutorial.with.jenkins.pipeline.title')}>
      <p className="sw-mb-4">{translate('onboarding.tutorial.with.jenkins.pipeline.intro')}</p>
      <NumberedList>
        <NumberedListItem>
          <SentenceWithHighlights
            highlightKeys={['new_item', 'type']}
            translationKey="onboarding.tutorial.with.jenkins.pipeline.step1"
          />
        </NumberedListItem>
        <NumberedListItem>
          {alm === AlmKeys.GitLab ? (
            <>
              <SentenceWithHighlights
                highlightKeys={['tab', 'option']}
                translationKey="onboarding.tutorial.with.jenkins.pipeline.gitlab.step2.trigger"
              />
              <UnorderedList className="sw-ml-12">
                <ListItem>
                  <SentenceWithHighlights
                    highlightKeys={['triggers', 'push_events']}
                    translationKey="onboarding.tutorial.with.jenkins.pipeline.gitlab.step2.pick_triggers"
                  />
                </ListItem>
                <ListItem>
                  <SentenceWithHighlights
                    highlightKeys={['advanced']}
                    translationKey="onboarding.tutorial.with.jenkins.pipeline.gitlab.step2.click_advanced"
                  />
                </ListItem>
                <ListItem>
                  <SentenceWithHighlights
                    highlightKeys={['secret_token', 'generate']}
                    translationKey="onboarding.tutorial.with.jenkins.pipeline.gitlab.step2.secret_token"
                  />
                </ListItem>
              </UnorderedList>
            </>
          ) : (
            <SentenceWithHighlights
              highlightKeys={['tab', 'option']}
              translationKey="onboarding.tutorial.with.jenkins.pipeline.step2"
            />
          )}
        </NumberedListItem>
        <NumberedListItem>
          <SentenceWithHighlights
            highlightKeys={['tab']}
            translationKey="onboarding.tutorial.with.jenkins.pipeline.step3"
          />
          <UnorderedList className="sw-ml-12">
            <ListItem>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.pipeline.step3.definition" />
            </ListItem>
            <ListItem>
              <SentenceWithHighlights
                highlightKeys={['label', 'branches_to_build']}
                translationKey="onboarding.tutorial.with.jenkins.pipeline.step3.scm"
              />
            </ListItem>
            <ListItem>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.pipeline.step3.script_path" />
            </ListItem>
          </UnorderedList>
        </NumberedListItem>
        <NumberedListItem>
          <SentenceWithHighlights
            highlightKeys={['save']}
            translationKey="onboarding.tutorial.with.jenkins.pipeline.step4"
          />
        </NumberedListItem>
      </NumberedList>
    </TutorialStep>
  );
}
