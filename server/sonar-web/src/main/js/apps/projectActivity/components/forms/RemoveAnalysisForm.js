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
// @flow
import React from 'react';
import Modal from '../../../../components/controls/Modal';
import { ActionsDropdownItem } from '../../../../components/controls/ActionsDropdown';
import { translate } from '../../../../helpers/l10n';
/*:: import type { Analysis } from '../../types'; */

/*::
type Props = {
  analysis: Analysis,
  deleteAnalysis: (analysis: string) => Promise<*>
};
*/

/*::
type State = {
  open: boolean,
  processing: boolean
};
*/

export default class RemoveAnalysisForm extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    open: false,
    processing: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  openForm = () => {
    this.setState({ open: true });
  };

  closeForm = () => {
    if (this.mounted) {
      this.setState({ open: false });
    }
  };

  stopProcessing = () => {
    if (this.mounted) {
      this.setState({ processing: false });
    }
  };

  stopProcessingAndClose = () => {
    if (this.mounted) {
      this.setState({ open: false, processing: false });
    }
  };

  handleSubmit = (e /*: Event */) => {
    e.preventDefault();
    this.setState({ processing: true });
    this.props
      .deleteAnalysis(this.props.analysis.key)
      .then(this.stopProcessingAndClose, this.stopProcessing);
  };

  renderModal() {
    const header = translate('project_activity.delete_analysis');
    return (
      <Modal key="delete-analysis-modal" contentLabel={header} onRequestClose={this.closeForm}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">{translate('project_activity.delete_analysis.question')}</div>

          <footer className="modal-foot">
            {this.state.processing ? (
              <i className="spinner" />
            ) : (
              <div>
                <button type="submit" className="button-red" autoFocus={true}>
                  {translate('delete')}
                </button>
                <button type="reset" className="button-link" onClick={this.closeForm}>
                  {translate('cancel')}
                </button>
              </div>
            )}
          </footer>
        </form>
      </Modal>
    );
  }

  render() {
    const linkComponent = (
      <ActionsDropdownItem
        className="js-delete-analysis"
        destructive={true}
        onClick={this.openForm}>
        {translate('project_activity.delete_analysis')}
      </ActionsDropdownItem>
    );
    if (this.state.open) {
      return [linkComponent, this.renderModal()];
    }
    return linkComponent;
  }
}
