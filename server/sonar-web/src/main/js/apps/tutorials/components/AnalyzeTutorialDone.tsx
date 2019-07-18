/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import BackButton from 'sonar-ui-common/components/controls/BackButton';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';

interface Props {
  setTutorialDone: (done: boolean) => void;
}

export default function AnalyzeTutorialDone({ setTutorialDone }: Props) {
  return (
    <div className="page-analysis-container page-analysis-container-sonarcloud">
      <BackButton
        onClick={() => setTutorialDone(false)}
        tooltip={translate('onboarding.tutorial.return_to_tutorial')}>
        {translate('back')}
      </BackButton>
      <div className="page-analysis page-analysis-waiting huge-spacer-top huge-spacer-bottom">
        <img
          alt="SonarCloud"
          src={`${getBaseUrl()}/images/sonarcloud/analysis/Waiting-for-analysis.svg`}
        />
        <h1 className="big-spacer-bottom huge-spacer-top">
          {translate('onboarding.finished.title')}
        </h1>
        <p>{translate('onboarding.finished.text')}</p>

        <div className="links huge-spacer-top huge-spacer-bottom">
          <h2 className="huge-spacer-bottom">{translate('onboarding.finished.links.title')}</h2>
          <ul>
            <li className="big-spacer-bottom">
              <a
                href="https://sonarcloud.io/documentation/user-guide/quality-gates/"
                rel="noopener noreferrer"
                target="_blank">
                What is a Quality Gate?
              </a>
            </li>
            <li className="big-spacer-bottom">
              <a
                href="https://sonarcloud.io/documentation/instance-administration/quality-profiles/"
                rel="noopener noreferrer"
                target="_blank">
                Configure your Quality Profiles
              </a>
              .
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
