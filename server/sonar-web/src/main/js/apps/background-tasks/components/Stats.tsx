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
import Tooltip from '../../../components/controls/Tooltip';
import { DeleteButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import ConfirmButton from '../../../components/controls/ConfirmButton';

interface Props {
  component?: unknown;
  failingCount?: number;
  isSystemAdmin?: boolean;
  pendingCount?: number;
  onShowFailing: () => void;
  onCancelAllPending: () => void;
}

export default class Stats extends React.PureComponent<Props> {
  handleShowFailing = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onShowFailing();
  };

  renderPending() {
    if (this.props.pendingCount === undefined) {
      return null;
    }
    if (this.props.pendingCount > 0) {
      return (
        <span>
          <span className="js-pending-count emphasised-measure">{this.props.pendingCount}</span>
          <span className="display-inline-flex-center little-spacer-left">
            {translate('background_tasks.pending')}
            {this.props.isSystemAdmin && (
              <ConfirmButton
                cancelButtonText={translate('close')}
                confirmButtonText={translate('background_tasks.cancel_all_tasks.submit')}
                data-test="cancel-pending"
                isDestructive={true}
                modalBody={translate('background_tasks.cancel_all_tasks.text')}
                modalHeader={translate('background_tasks.cancel_all_tasks')}
                onConfirm={this.props.onCancelAllPending}>
                {({ onClick }) => (
                  <Tooltip overlay={translate('background_tasks.cancel_all_tasks')}>
                    <DeleteButton
                      className="js-cancel-pending little-spacer-left"
                      onClick={onClick}
                    />
                  </Tooltip>
                )}
              </ConfirmButton>
            )}
          </span>
        </span>
      );
    } else {
      return (
        <span>
          <span className="js-pending-count emphasised-measure">{this.props.pendingCount}</span>
          &nbsp;
          {translate('background_tasks.pending')}
        </span>
      );
    }
  }

  renderFailures() {
    if (this.props.failingCount === undefined) {
      return null;
    }

    if (this.props.component) {
      return null;
    }

    if (this.props.failingCount > 0) {
      return (
        <span>
          <Tooltip overlay={translate('background_tasks.failing_count')}>
            <a
              className="js-failures-count emphasised-measure"
              href="#"
              onClick={this.handleShowFailing}>
              {this.props.failingCount}
            </a>
          </Tooltip>
          &nbsp;
          {translate('background_tasks.failures')}
        </span>
      );
    } else {
      return (
        <span>
          <Tooltip overlay={translate('background_tasks.failing_count')}>
            <span className="js-failures-count emphasised-measure">{this.props.failingCount}</span>
          </Tooltip>
          <span className="little-spacer-left">{translate('background_tasks.failures')}</span>
        </span>
      );
    }
  }

  render() {
    return (
      <section className="big-spacer-top big-spacer-bottom">
        <span>{this.renderPending()}</span>
        <span className="huge-spacer-left">{this.renderFailures()}</span>
      </section>
    );
  }
}
