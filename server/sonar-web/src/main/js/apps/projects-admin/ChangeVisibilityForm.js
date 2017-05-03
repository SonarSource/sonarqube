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
import Modal from 'react-modal';
import classNames from 'classnames';
import UpgradeOrganizationBox from '../../components/common/UpgradeOrganizationBox';
import { translate } from '../../helpers/l10n';
import type { Organization } from '../../store/organizations/duck';

type Props = {
  onClose: () => void,
  onConfirm: string => void,
  organization: Organization
};

type State = {
  visibility: string
};

export default class ChangeVisibilityForm extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { visibility: props.organization.projectVisibility };
  }

  handleCancelClick = (event: Event) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleConfirmClick = (event: Event) => {
    event.preventDefault();
    this.props.onConfirm(this.state.visibility);
    this.props.onClose();
  };

  handleVisibilityClick = (visibility: string) => (
    event: Event & { currentTarget: HTMLElement }
  ) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ visibility });
  };

  render() {
    const { canUpdateProjectsVisibilityToPrivate } = this.props.organization;

    return (
      <Modal
        isOpen={true}
        contentLabel="modal form"
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>

        <header className="modal-head">
          <h2>{translate('organization.change_visibility_form.header')}</h2>
        </header>

        <div className="modal-body">
          {['public', 'private'].map(visibility => (
            <div className="big-spacer-bottom" key={visibility}>
              <p>
                {visibility === 'private' && !canUpdateProjectsVisibilityToPrivate
                  ? <span className="text-muted cursor-not-allowed">
                      <i
                        className={classNames('icon-radio', 'spacer-right', {
                          'is-checked': this.state.visibility === visibility
                        })}
                      />
                      {translate('visibility', visibility)}
                    </span>
                  : <a
                      className="link-base-color link-no-underline"
                      href="#"
                      onClick={this.handleVisibilityClick(visibility)}>
                      <i
                        className={classNames('icon-radio', 'spacer-right', {
                          'is-checked': this.state.visibility === visibility
                        })}
                      />
                      {translate('visibility', visibility)}
                    </a>}
              </p>
              <p className="text-muted spacer-top" style={{ paddingLeft: 22 }}>
                {translate('visibility', visibility, 'description.short')}
              </p>
            </div>
          ))}

          {canUpdateProjectsVisibilityToPrivate
            ? <div className="alert alert-warning">
                {translate('organization.change_visibility_form.warning')}
              </div>
            : <UpgradeOrganizationBox organization={this.props.organization.key} />}
        </div>

        <footer className="modal-foot">
          <button onClick={this.handleConfirmClick}>
            {translate('organization.change_visibility_form.submit')}
          </button>
          <a href="#" onClick={this.handleCancelClick}>{translate('cancel')}</a>
        </footer>

      </Modal>
    );
  }
}
