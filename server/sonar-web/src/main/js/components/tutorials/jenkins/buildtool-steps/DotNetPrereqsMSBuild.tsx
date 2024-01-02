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
import { ClipboardIconButton } from '../../../../components/controls/clipboard';
import { Alert } from '../../../../components/ui/Alert';
import { translate } from '../../../../helpers/l10n';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';

export default function DotNetPrereqsMSBuild() {
  return (
    <li className="abs-width-600">
      <SentenceWithHighlights
        highlightKeys={['default_msbuild']}
        translationKey="onboarding.tutorial.with.jenkins.dotnet.msbuild.prereqs.title"
      />
      <Alert className="spacer-top" variant="info">
        {translate('onboarding.tutorial.with.jenkins.dotnet.msbuild.prereqs.info')}
      </Alert>
      <ol className="list-styled list-roman">
        <li>
          <SentenceWithHighlights
            highlightKeys={['msbuild']}
            translationKey="onboarding.tutorial.with.jenkins.dotnet.msbuild.prereqs.step1"
          />
        </li>
        <li>
          <SentenceWithHighlights
            highlightKeys={['path']}
            translationKey="onboarding.tutorial.with.jenkins.dotnet.msbuild.prereqs.step2"
          />
        </li>
        <li>
          <SentenceWithHighlights
            highlightKeys={['msbuild', 'add_msbuild', 'name', 'msbuild_plugin']}
            translationKey="onboarding.tutorial.with.jenkins.dotnet.msbuild.prereqs.step3"
          />
          <code className="rule">Default MSBuild</code>
          <ClipboardIconButton copyValue="Default MSBuild" />
        </li>
      </ol>
    </li>
  );
}
