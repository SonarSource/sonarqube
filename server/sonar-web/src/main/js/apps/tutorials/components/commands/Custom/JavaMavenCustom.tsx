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
import { FormattedMessage } from 'react-intl';
import { Button, EditButton } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../../components/common/CodeSnippet';
import InstanceMessage from '../../../../../components/common/InstanceMessage';
import { ProjectAnalysisModes } from '../../ProjectAnalysisStepFromBuildTool';

export interface JavaCustomProps {
  host: string;
  mode: ProjectAnalysisModes;
  onDone: VoidFunction;
  organization?: string;
  projectKey?: string;
  toggleModal: VoidFunction;
  token: string;
}

interface RenderCustomCommandProps {
  command: (string | undefined)[];
  toggleModal: VoidFunction;
}

export function RenderCustomCommand({
  command,
  toggleModal
}: RenderCustomCommandProps): JSX.Element {
  return (
    <>
      {command.join(' \\\n  ')}{' '}
      <EditButton className="edit-token spacer-left" onClick={toggleModal} />
    </>
  );
}

interface RenderCustomContent {
  linkText: string;
  linkUrl: string;
  onDone: VoidFunction;
}

export function RenderCustomContent({
  command,
  linkText,
  linkUrl,
  onDone,
  toggleModal
}: RenderCustomCommandProps & RenderCustomContent) {
  return (
    <>
      <CodeSnippet
        render={() => <RenderCustomCommand command={command} toggleModal={toggleModal} />}
        snippet={command}
        wrap={true}
      />

      <p className="big-spacer-top markdown">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.docs')}
          id="onboarding.analysis.docs"
          values={{
            link: (
              <a href={linkUrl} rel="noopener noreferrer" target="_blank">
                {translate(linkText)}
              </a>
            )
          }}
        />
      </p>

      <div className="big-spacer-top">
        <Button className="js-continue" onClick={onDone}>
          {translate('onboarding.finish')}
        </Button>
      </div>
    </>
  );
}

export default function JavaMavenCustom(props: JavaCustomProps) {
  const suffix = props.mode === ProjectAnalysisModes.CI ? '.ci' : '';
  const command = [
    'mvn sonar:sonar',
    props.projectKey && `-Dsonar.projectKey=${props.projectKey}`,
    props.organization && `-Dsonar.organization=${props.organization}`,
    `-Dsonar.host.url=${props.host}`,
    `-Dsonar.login=${props.token}`
  ];

  return (
    <div>
      <h4 className="spacer-bottom">
        {translate(`onboarding.analysis.java.maven.header${suffix}`)}
      </h4>

      <p className="spacer-bottom markdown">
        <InstanceMessage
          message={translate(`onboarding.analysis.java.maven.text.custom${suffix}`)}
        />
      </p>

      <RenderCustomContent
        command={command}
        linkText="onboarding.analysis.java.maven.docs_link"
        linkUrl="http://redirect.sonarsource.com/doc/install-configure-scanner-maven.html"
        onDone={props.onDone}
        toggleModal={props.toggleModal}
      />
    </div>
  );
}
