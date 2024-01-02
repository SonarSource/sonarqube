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
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../../helpers/l10n';
import CodeSnippet from '../../../common/CodeSnippet';
import DocLink from '../../../common/DocLink';
import { OSs } from '../../types';

export interface ExecBuildWrapperProps {
  os: OSs;
}

const executables: { [x in OSs]: string } = {
  linux: 'build-wrapper-linux-x86-64',
  win: 'build-wrapper-win-x86-64.exe',
  mac: 'build-wrapper-macosx-x86',
};

export default function ExecBuildWrapper(props: ExecBuildWrapperProps) {
  const { os } = props;

  return (
    <>
      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.build_wrapper.execute')}
      </h4>
      <p className="spacer-bottom markdown">
        {translate('onboarding.analysis.build_wrapper.execute_text')}
      </p>
      <CodeSnippet
        snippet={`${executables[os]} --out-dir bw-output ${translate(
          'onboarding.analysis.build_wrapper.execute_build_command'
        )}`}
      />
      <p className="big-spacer-top markdown">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.build_wrapper.docs')}
          id="onboarding.analysis.build_wrapper.docs"
          values={{
            link: (
              <DocLink to="/analyzing-source-code/languages/c-family/">
                {translate('onboarding.analysis.build_wrapper.docs_link')}
              </DocLink>
            ),
          }}
        />
      </p>
    </>
  );
}
