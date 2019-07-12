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
import { Link } from 'react-router';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';

export default function AccountDeleted() {
  return (
    <div className="page-wrapper-simple display-flex-column">
      <Alert className="huge-spacer-bottom" variant="success">
        {translate('my_profile.delete_account.success')}
      </Alert>

      <div className="page-simple text-center">
        <p className="big-spacer-bottom">
          <h1>{translate('my_profile.delete_account.feedback.reason.explanation')}</h1>
        </p>
        <p className="spacer-bottom">
          <FormattedMessage
            defaultMessage={translate('my_profile.delete_account.feedback.call_to_action')}
            id="my_profile.delete_account.feedback.call_to_action"
            values={{
              link: <Link to="/about/contact">{translate('footer.contact_us')}</Link>
            }}
          />
        </p>
        <p>
          <Link to="/">{translate('go_back_to_homepage')}</Link>
        </p>
      </div>
    </div>
  );
}
