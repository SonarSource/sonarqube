/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as classNames from 'classnames';
import { Organization, Visibility } from '../../app/types';
import UpgradeOrganizationBox from '../../components/common/UpgradeOrganizationBox';
import { translate } from '../../helpers/l10n';
import Modal from '../../components/controls/Modal';

export interface Props {
  onClose: () => void;
  onConfirm: (visiblity: Visibility) => void;
  organization: Organization;
}

interface State {
  visibility: Visibility;
}

export default class ChangeVisibilityForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { visibility: props.organization.projectVisibility as Visibility };
  }

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleConfirmClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    this.props.onConfirm(this.state.visibility);
    this.props.onClose();
  };

  handleVisibilityClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    const visibility = event.currentTarget.dataset.visibility as Visibility;
    this.setState({ visibility });
  };

  render() {
    const { canUpdateProjectsVisibilityToPrivate } = this.props.organization;

    return (
      <Modal contentLabel="modal form" onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{translate('organization.change_visibility_form.header')}</h2>
        </header>

        <div className="modal-body">
          {[Visibility.Public, Visibility.Private].map(visibility => (
            <div className="big-spacer-bottom" key={visibility}>
              <p>
                {visibility === Visibility.Private && !canUpdateProjectsVisibilityToPrivate ? (
                  <span className="text-muted cursor-not-allowed">
                    <i
                      className={classNames('icon-radio', 'spacer-right', {
                        'is-checked': this.state.visibility === visibility
                      })}
                    />
                    {translate('visibility', visibility)}
                  </span>
                ) : (
                  <a
                    className="link-base-color link-no-underline"
                    data-visibility={visibility}
                    href="#"
                    onClick={this.handleVisibilityClick}>
                    <i
                      className={classNames('icon-radio', 'spacer-right', {
                        'is-checked': this.state.visibility === visibility
                      })}
                    />
                    {translate('visibility', visibility)}
                  </a>
                )}
              </p>
              <p className="text-muted spacer-top" style={{ paddingLeft: 22 }}>
                {translate('visibility', visibility, 'description.short')}
              </p>
            </div>
          ))}

          {canUpdateProjectsVisibilityToPrivate ? (
            <div className="alert alert-warning">
              {translate('organization.change_visibility_form.warning')}
            </div>
          ) : (
            <UpgradeOrganizationBox organization={this.props.organization.key} />
          )}
        </div>

        <footer className="modal-foot">
          <button className="js-confirm" onClick={this.handleConfirmClick}>
            {translate('organization.change_visibility_form.submit')}
          </button>
          <a className="js-modal-close" href="#" onClick={this.handleCancelClick}>
            {translate('cancel')}
          </a>
        </footer>
      </Modal>
    );
  }
}
