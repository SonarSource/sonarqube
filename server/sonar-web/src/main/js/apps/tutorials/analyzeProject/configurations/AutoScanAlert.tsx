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
import { Alert } from '../../../../components/ui/Alert';
import DocTooltip from '../../../../components/docs/DocTooltip';
import { translate } from '../../../../helpers/l10n';

const caveats = {
  default: `
No visual feedback (yet) in the UI.  
Only Pull Requests from the same repository are analyzed.  
Not supported: code coverage, import of external rule engine reports.

---

[Read more](https://sonarcloud.io/documentation/autoscan/) and join [the forum](https://community.sonarsource.com/tags/c/help/sc/autoscan) to ask your questions
`
};

const limitedScope = {
  default: `
The following languages are currently supported:

ABAP, Apex, CSS, Flex, Go, HTML, JS, Kotlin, PHP, Python, Ruby, Scala, Swift, TypeScript, TSQL, XML.
`
};

export function AutoScanAlert() {
  return (
    <Alert className="big-spacer-top" variant="info">
      <div>
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.with.autoscan.alert')}
          id="onboarding.analysis.with.autoscan.alert"
          values={{
            caveats: (
              <>
                <strong>{translate('onboarding.analysis.with.autoscan.alert.caveats')}</strong>{' '}
                <DocTooltip doc={Promise.resolve(caveats)} />
              </>
            ),
            scopes: (
              <>
                <strong>{translate('onboarding.analysis.with.autoscan.alert.scopes')}</strong>{' '}
                <DocTooltip doc={Promise.resolve(limitedScope)} />
              </>
            )
          }}
        />
      </div>
    </Alert>
  );
}
