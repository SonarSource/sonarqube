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

import { CodeSnippet, Link, SubHeading } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { DocLink } from '../../../../helpers/doc-links';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
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

  const docUrl = useDocUrl(DocLink.CFamily);

  return (
    <>
      <SubHeading className="sw-mt-8 sw-mb-2">
        {translate('onboarding.analysis.build_wrapper.execute')}
      </SubHeading>
      <p className="sw-mb-2">{translate('onboarding.analysis.build_wrapper.execute_text')}</p>
      <CodeSnippet
        className="sw-px-4"
        isOneLine
        snippet={`${executables[os]} --out-dir bw-output ${translate(
          'onboarding.analysis.build_wrapper.execute_build_command',
        )}`}
      />
      <p className="sw-mt-4">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.build_wrapper.docs')}
          id="onboarding.analysis.build_wrapper.docs"
          values={{
            link: (
              <Link to={docUrl}>{translate('onboarding.analysis.build_wrapper.docs_link')}</Link>
            ),
          }}
        />
      </p>
    </>
  );
}
