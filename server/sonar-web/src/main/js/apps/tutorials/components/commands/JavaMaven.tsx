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
import CodeSnippet from '../../../../components/common/CodeSnippet';
import InstanceMessage from '../../../../components/common/InstanceMessage';
import { translate } from '../../../../helpers/l10n';

interface Props {
  host: string;
  organization?: string;
  projectKey?: string;
  token: string;
}

export default function JavaMaven(props: Props) {
  const command = [
    'mvn sonar:sonar',
    props.projectKey && `-Dsonar.projectKey=${props.projectKey}`,
    props.organization && `-Dsonar.organization=${props.organization}`,
    `-Dsonar.host.url=${props.host}`,
    `-Dsonar.login=${props.token}`
  ];

  return (
    <div>
      <h4 className="spacer-bottom">{translate('onboarding.analysis.java.maven.header')}</h4>
      <p className="spacer-bottom markdown">
        <InstanceMessage message={translate('onboarding.analysis.java.maven.text')} />
      </p>
      <CodeSnippet snippet={command} />
      <p
        className="big-spacer-top markdown"
        dangerouslySetInnerHTML={{ __html: translate('onboarding.analysis.java.maven.docs') }}
      />
      <p
        className="big-spacer-top markdown"
        dangerouslySetInnerHTML={{
          __html: props.projectKey
            ? translate('onboarding.analysis.auto_refresh_after_analysis')
            : translate('onboarding.analysis.browse_url_after_analysis')
        }}
      />
    </div>
  );
}
