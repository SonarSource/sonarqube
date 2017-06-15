/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { translate } from '../../../../helpers/l10n';

type Props = {
  className?: string,
  os: string
};

export default function SQScanner(props: Props) {
  return (
    <div className={props.className}>
      <h4 className="spacer-bottom">
        {translate('onboarding.analysis.sq_scanner.header', props.os)}
      </h4>
      <p
        className="spacer-bottom markdown"
        dangerouslySetInnerHTML={{
          __html: translate('onboarding.analysis.sq_scanner.text', props.os)
        }}
      />
      <p>
        <a
          className="button"
          href="http://redirect.sonarsource.com/doc/install-configure-scanner.html"
          target="_blank">
          {translate('download_verb')}
        </a>
      </p>
    </div>
  );
}
