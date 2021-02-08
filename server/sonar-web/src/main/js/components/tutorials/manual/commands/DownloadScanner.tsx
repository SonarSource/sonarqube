/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { OSs } from '../../types';

interface Props {
  os: OSs;
}

export default function SQScanner(props: Props) {
  const { os } = props;
  return (
    <div>
      <h4 className="spacer-bottom">{translate('onboarding.analysis.sq_scanner.header', os)}</h4>
      <p className="spacer-bottom markdown">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.sq_scanner.text')}
          id="onboarding.analysis.sq_scanner.text"
          values={{
            dir: <code>bin</code>,
            env_var: <code>{os === OSs.Windows ? '%PATH%' : 'PATH'}</code>
          }}
        />
      </p>
      <p>
        <Link className="button" to="/documentation/analysis/scan/sonarscanner/" target="_blank">
          {translate('download_verb')}
        </Link>
      </p>
    </div>
  );
}
