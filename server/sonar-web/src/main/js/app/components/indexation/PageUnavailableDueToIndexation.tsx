/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { FlagMessage, Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import withIndexationContext, {
  WithIndexationContextProps,
} from '../../../components/hoc/withIndexationContext';
import { translate } from '../../../helpers/l10n';

export class PageUnavailableDueToIndexation extends React.PureComponent<WithIndexationContextProps> {
  componentDidUpdate() {
    if (
      this.props.indexationContext.status.isCompleted &&
      !this.props.indexationContext.status.hasFailures
    ) {
      window.location.reload();
    }
  }

  render() {
    return (
      <div className="page-wrapper-simple">
        <FlagMessage className="sw-m-10" variant="info">
          {translate('indexation.page_unavailable.description')}
          <br />
          <FormattedMessage
            defaultMessage={translate(
              'indexation.page_unavailable.description.additional_information'
            )}
            id="indexation.page_unavailable.description.additional_information"
            values={{
              link: (
                <Link
                  className="sw-ml-4"
                  to="https://docs.sonarqube.org/latest/instance-administration/reindexing/"
                >
                  {translate('learn_more')}
                </Link>
              ),
            }}
          />
        </FlagMessage>
      </div>
    );
  }
}

export default withIndexationContext(PageUnavailableDueToIndexation);
