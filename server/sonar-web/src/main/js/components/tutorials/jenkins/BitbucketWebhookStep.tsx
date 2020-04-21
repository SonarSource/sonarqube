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
import { FormattedMessage } from 'react-intl';
import { Button, ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  BitbucketBindingDefinition,
  ProjectBitbucketBindingResponse
} from '../../../types/alm-settings';
import CodeSnippet from '../../common/CodeSnippet';
import LabelActionPair from '../components/LabelActionPair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import Step from '../components/Step';

export interface BitbucketWebhookStepProps {
  almBinding?: BitbucketBindingDefinition;
  finished: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
  projectBinding: ProjectBitbucketBindingResponse;
}

export default function BitbucketWebhookStep(props: BitbucketWebhookStepProps) {
  const { almBinding, finished, open, projectBinding } = props;
  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={() => (
        <div className="boxed-group-inner">
          <p className="big-spacer-bottom">
            <FormattedMessage
              defaultMessage={translate(
                'onboarding.tutorial.with.jenkins.bitbucket_webhook.intro.sentence'
              )}
              id="onboarding.tutorial.with.jenkins.bitbucket_webhook.intro.sentence"
              values={{
                link: (
                  <ButtonLink onClick={props.onDone}>
                    {translate('onboarding.tutorial.with.jenkins.bitbucket_webhook.intro.link')}
                  </ButtonLink>
                )
              }}
            />
          </p>
          <ol className="list-styled">
            <li>
              <FormattedMessage
                defaultMessage={translate(
                  'onboarding.tutorial.with.jenkins.bitbucket_webhook.step1.sentence'
                )}
                id="onboarding.tutorial.with.jenkins.bitbucket_webhook.step1.sentence"
                values={{
                  link:
                    almBinding !== undefined ? (
                      <a
                        href={`${almBinding.url.replace(
                          /\/$/,
                          ''
                        )}/plugins/servlet/webhooks/projects/${projectBinding.repository}/repos/${
                          projectBinding.slug
                        }/create`}
                        rel="noopener noreferrer"
                        target="_blank">
                        {translate('onboarding.tutorial.with.jenkins.bitbucket_webhook.step1.link')}
                      </a>
                    ) : (
                      translate('onboarding.tutorial.with.jenkins.bitbucket_webhook.step1.link')
                    )
                }}
              />
              <ul className="list-styled">
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.bitbucket_webhook.step1.name" />
                </li>
                <li className="abs-width-600">
                  <p>
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.bitbucket_webhook.step1.url" />
                  </p>
                  <CodeSnippet
                    isOneLine={true}
                    snippet={`***JENKINS_URL***/bitbucket-scmsource-hook/notify?server_url=${
                      almBinding !== undefined ? almBinding.url : '***BITBUCKET_URL***'
                    }`}
                  />
                  <Alert variant="info">
                    {translate(
                      'onboarding.tutorial.with.jenkins.bitbucket_webhook.step1.url.warning'
                    )}
                  </Alert>
                </li>
              </ul>
            </li>
            <li>
              <SentenceWithHighlights
                highlightKeys={['events']}
                translationKey="onboarding.tutorial.with.jenkins.bitbucket_webhook.step2"
              />
              <ul className="list-styled">
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.bitbucket_webhook.step2.repo" />
                </li>
                <li>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.bitbucket_webhook.step2.pr" />
                </li>
              </ul>
            </li>
            <li>
              <SentenceWithHighlights
                highlightKeys={['create']}
                translationKey="onboarding.tutorial.with.jenkins.bitbucket_webhook.step3"
              />
            </li>
          </ol>
          <Button onClick={props.onDone}>{translate('continue')}</Button>
        </div>
      )}
      stepNumber={2}
      stepTitle={translate('onboarding.tutorial.with.jenkins.bitbucket_webhook.title')}
    />
  );
}
