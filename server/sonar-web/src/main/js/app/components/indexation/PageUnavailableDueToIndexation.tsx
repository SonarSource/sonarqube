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
import withIndexationContext, {
  WithIndexationContextProps,
} from '../../../components/hoc/withIndexationContext';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';

interface Props extends WithIndexationContextProps {
  pageContext?: PageContext;
  component?: Pick<Component, 'qualifier' | 'name'>;
}

export enum PageContext {
  Issues = 'issues',
  Portfolios = 'portfolios',
}

export class PageUnavailableDueToIndexation extends React.PureComponent<Props> {
  componentDidUpdate() {
    if (
      this.props.indexationContext.status.isCompleted &&
      !this.props.indexationContext.status.hasFailures
    ) {
      window.location.reload();
    }
  }

  render() {
    const { pageContext, component } = this.props;
    let messageKey = 'indexation.page_unavailable.title';

    if (pageContext) {
      messageKey = `${messageKey}.${pageContext}`;
    }

    return (
      <div className="page-wrapper-simple">
        <div className="page-simple">
          <h1 className="big-spacer-bottom">
            <FormattedMessage
              id={messageKey}
              defaultMessage={translate(messageKey)}
              values={{
                componentQualifier: translate('qualifier', component?.qualifier ?? ''),
                componentName: <em>{component?.name}</em>,
              }}
            />
          </h1>
          <Alert variant="info">
            <p>{translate('indexation.page_unavailable.description')}</p>
            <p className="spacer-top">
              {translate('indexation.page_unavailable.description.additional_information')}
            </p>
          </Alert>
        </div>
      </div>
    );
  }
}

export default withIndexationContext(PageUnavailableDueToIndexation);
