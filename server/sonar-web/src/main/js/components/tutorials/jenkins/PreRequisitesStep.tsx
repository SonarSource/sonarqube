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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { rawSizes } from '../../../app/theme';
import { Button } from '../../../components/controls/buttons';
import Checkbox from '../../../components/controls/Checkbox';
import ChevronRightIcon from '../../../components/icons/ChevronRightIcon';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import Step from '../components/Step';

export interface PreRequisitesStepProps {
  alm: AlmKeys;
  branchesEnabled: boolean;
  finished: boolean;
  onChangeSkipNextTime: (skip: boolean) => void;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
  skipNextTime: boolean;
}

export default function PreRequisitesStep(props: PreRequisitesStepProps) {
  const { alm, branchesEnabled, finished, open, skipNextTime } = props;
  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={() => (
        <div className="boxed-group-inner">
          <Alert className="big-spacer-bottom" variant="warning">
            <SentenceWithHighlights
              highlightKeys={['installed', 'configured']}
              translationKey="onboarding.tutorial.with.jenkins.prereqs.intro"
            />
          </Alert>
          <ul className="list-styled big-spacer-bottom">
            {branchesEnabled && (
              <li>
                {translate('onboarding.tutorial.with.jenkins.prereqs.plugins.branch_source', alm)}
              </li>
            )}
            {!branchesEnabled && alm === AlmKeys.GitLab && (
              <li>{translate('onboarding.tutorial.with.jenkins.prereqs.plugins.gitlab_plugin')}</li>
            )}
            <li>{translate('onboarding.tutorial.with.jenkins.prereqs.plugins.sonar_scanner')}</li>
          </ul>
          <p className="big-spacer-bottom">
            <FormattedMessage
              defaultMessage={translate(
                'onboarding.tutorial.with.jenkins.prereqs.step_by_step_guide'
              )}
              id="onboarding.tutorial.with.jenkins.prereqs.step_by_step_guide"
              values={{
                link: (
                  <Link target="_blank" to="/documentation/analysis/jenkins/">
                    {translate('onboarding.tutorial.with.jenkins.prereqs.step_by_step_guide.link')}
                  </Link>
                )
              }}
            />
          </p>
          <p className="big-spacer-bottom">
            {translate('onboarding.tutorial.with.jenkins.prereqs.following_are_recommendations')}
          </p>
          <p className="big-spacer-bottom display-flex-center">
            <label
              className="cursor-pointer"
              htmlFor="skip-prereqs"
              onClick={() => {
                props.onChangeSkipNextTime(!skipNextTime);
              }}>
              {translate('onboarding.tutorial.with.jenkins.prereqs.skip_next_time')}
            </label>
            <Checkbox
              checked={skipNextTime}
              className="little-spacer-left"
              id="skip-prereqs"
              onCheck={props.onChangeSkipNextTime}
            />
          </p>
          <Button className="big-spacer-top" onClick={props.onDone}>
            {translate('onboarding.tutorial.with.jenkins.prereqs.done')}
            <ChevronRightIcon size={rawSizes.baseFontSizeRaw} />
          </Button>
        </div>
      )}
      stepTitle={translate('onboarding.tutorial.with.jenkins.prereqs.title')}
    />
  );
}
