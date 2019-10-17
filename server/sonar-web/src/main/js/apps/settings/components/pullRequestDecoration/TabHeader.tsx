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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALM_KEYS } from '../../utils';

export interface TabHeaderProps {
  alm: ALM_KEYS;
  onCreate: () => void;
}

export default function TabHeader(props: TabHeaderProps) {
  const { alm } = props;
  return (
    <>
      <Alert className="spacer-top huge-spacer-bottom" variant="info">
        <FormattedMessage
          defaultMessage={translate(`settings.pr_decoration.${alm}.info`)}
          id={`settings.pr_decoration.${alm}.info`}
          values={{
            link: (
              <Link to="/documentation/analysis/pull-request/#pr-decoration">
                {translate('learn_more')}
              </Link>
            )
          }}
        />
      </Alert>

      <div className="big-spacer-bottom display-flex-space-between">
        <h4 className="display-inline">{translate('settings.pr_decoration.table.title')}</h4>
        <Button onClick={props.onCreate}>{translate('settings.pr_decoration.table.create')}</Button>
      </div>
    </>
  );
}
