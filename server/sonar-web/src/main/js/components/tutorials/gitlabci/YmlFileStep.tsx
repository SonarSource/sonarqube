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
import { Link } from 'react-router';
import { ClipboardIconButton } from 'sonar-ui-common/components/controls/clipboard';
import { translate } from 'sonar-ui-common/helpers/l10n';
import Step from '../components/Step';
import PipeCommandGradle from './commands/PipeCommandGradle';
import PipeCommandMaven from './commands/PipeCommandMaven';
import PipeCommandOther from './commands/PipeCommandOther';
import { BuildTools } from './types';

export interface YmlFileStepProps {
  buildTool?: BuildTools;
  open: boolean;
}

export default function YmlFileStep({ buildTool, open }: YmlFileStepProps) {
  const renderForm = () => (
    <div className="boxed-group-inner">
      <div className="flex-columns">
        <div className="flex-column-full">
          {buildTool && (
            <>
              <div className="big-spacer-bottom">
                <FormattedMessage
                  defaultMessage={translate('onboarding.tutorial.with.gitlab_ci.yml.description')}
                  id="onboarding.tutorial.with.gitlab_ci.yml.description"
                  values={{
                    filename: (
                      <>
                        <code className="rule">
                          {translate('onboarding.tutorial.with.gitlab_ci.yml.filename')}
                        </code>
                        <ClipboardIconButton
                          className="little-spacer-left"
                          copyValue={translate('onboarding.tutorial.with.gitlab_ci.yml.filename')}
                        />
                      </>
                    )
                  }}
                />
              </div>

              <div className="big-spacer-bottom">
                {buildTool === BuildTools.Maven && <PipeCommandMaven />}
                {buildTool === BuildTools.Gradle && <PipeCommandGradle />}
                {buildTool === BuildTools.Other && <PipeCommandOther />}
              </div>

              <p className="little-spacer-bottom">
                {translate('onboarding.tutorial.with.gitlab_ci.yml.baseconfig')}
              </p>

              <p className="huge-spacer-bottom">
                {translate('onboarding.tutorial.with.gitlab_ci.yml.existing')}
              </p>

              <hr className="no-horizontal-margins" />
              <div>
                <p className="big-spacer-bottom">
                  <strong>{translate('onboarding.tutorial.with.gitlab_ci.yml.done')} </strong>{' '}
                  <FormattedMessage
                    defaultMessage={translate(
                      'onboarding.tutorial.with.gitlab_ci.yml.done.description'
                    )}
                    id="onboarding.tutorial.with.gitlab_ci.yml.done.description"
                    values={{
                      /* This link will be added when the backend provides the project URL */
                      link: translate(
                        'onboarding.tutorial.with.gitlab_ci.yml.done.description.link'
                      )
                    }}
                  />
                </p>
                <p className="big-spacer-bottom">
                  <strong>
                    {translate('onboarding.tutorial.with.gitlab_ci.yml.done.then-what')}
                  </strong>{' '}
                  {translate('onboarding.tutorial.with.gitlab_ci.yml.done.then-what.description')}
                </p>

                <p className="big-spacer-bottom">
                  <FormattedMessage
                    defaultMessage={translate(
                      'onboarding.tutorial.with.gitlab_ci.yml.done.links.title'
                    )}
                    id="onboarding.tutorial.with.gitlab_ci.yml.done.links.title"
                    values={{
                      links: (
                        <Link
                          rel="noopener noreferrer"
                          target="_blank"
                          to="/documentation/user-guide/quality-gates/">
                          {translate('onboarding.tutorial.with.gitlab_ci.yml.done.links.QG')}
                        </Link>
                      )
                    }}
                  />
                </p>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );

  return (
    <Step
      finished={false}
      open={open}
      renderForm={renderForm}
      stepNumber={3}
      stepTitle={translate('onboarding.tutorial.with.gitlab_ci.yml.title')}
    />
  );
}
