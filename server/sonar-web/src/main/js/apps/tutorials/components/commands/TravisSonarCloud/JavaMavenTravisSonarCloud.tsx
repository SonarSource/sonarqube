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
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../../components/common/CodeSnippet';
import {
  getSonarcloudAddonYml,
  getSonarcloudAddonYmlRender,
  RequirementJavaBuild
} from '../AnalysisCommandTravis';
import { Props } from '../JavaMaven';

export function JavaMavenTravisSonarCloud(props: Props) {
  const command = `${getSonarcloudAddonYml(props.organization)}

script:
  # the following command line builds the project, runs the tests with coverage and then execute the SonarCloud analysis
  - mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar`;

  const renderCommand = () => (
    <>
      {getSonarcloudAddonYmlRender(props.organization)}
      <br />
      {`script:
  # the following command line builds the project, runs the tests with coverage and then execute the SonarCloud analysis
  - mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -Dsonar.projectKey=${
    props.projectKey
  }`}
    </>
  );

  return (
    <div>
      <h2 className="spacer-bottom spacer-top">
        {translate('onboarding.analysis.with.travis.setup.title.a')}
      </h2>

      <RequirementJavaBuild />

      <hr className="no-horizontal-margins" />

      <h2 className="spacer-bottom spacer-top">
        {translate('onboarding.analysis.with.travis.setup.title.b')}
      </h2>

      <p
        className="spacer-bottom markdown"
        dangerouslySetInnerHTML={{
          __html: translate('onboarding.analysis.java.maven.text.sonarcloud')
        }}
      />

      <CodeSnippet render={renderCommand} snippet={command} />

      <p
        className="big-spacer-top markdown"
        dangerouslySetInnerHTML={{
          __html: translate('onboarding.analysis.java.maven.docs.sonarcloud')
        }}
      />
    </div>
  );
}
