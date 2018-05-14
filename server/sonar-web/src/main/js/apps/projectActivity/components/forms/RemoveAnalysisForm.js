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
import { translate } from '../../../../helpers/l10n';
import { SubmitButton, ResetButtonLink } from '../../../../components/ui/buttons';
/*:: import type { Analysis } from '../../types'; */

/*::
type Props = {
  analysis: Analysis,
  deleteAnalysis: (analysis: string) => Promise<*>,
  onClose: () => void;
};
*/

/*::
type State = {
  processing: boolean
};
*/

export default class RemoveAnalysisForm extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    processing: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopProcessing = () => {
    if (this.mounted) {
      this.setState({ processing: false });
    }
  };

  handleSubmit = (e /*: Event */) => {
    e.preventDefault();
    this.setState({ processing: true });
    this.props
      .deleteAnalysis(this.props.analysis.key)
      .then(this.props.onClose, this.stopProcessing);
  };

  render() {
    const header = translate('project_activity.delete_analysis');
    return (
      <Modal contentLabel={header} key="delete-analysis-modal" onRequestClose={this.props.onClose}>
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
                <SubmitButton autoFocus={true} className="button-red">
                  {translate('delete')}
                </SubmitButton>
                <ResetButtonLink onClick={this.props.onClose}>
                  {translate('cancel')}
                </ResetButtonLink>
              </div>
            )}
          </footer>
        </form>
      </Modal>
    );
  }
}
