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
import { connect } from 'react-redux';
import Modal from 'react-modal';
import type { Analysis } from '../../../../store/projectActivity/duck';
import { translate } from '../../../../helpers/l10n';
import { deleteAnalysis } from '../../actions';

type Props = {
  analysis: Analysis,
  deleteAnalysis: () => Promise<*>,
  project: string
};

type State = {
  open: boolean,
  processing: boolean
};

class RemoveAnalysisForm extends React.Component {
  mounted: boolean;
  props: Props;

  state: State = {
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
    if (this.mounted) {
      this.setState({ open: true });
    }
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

  handleSubmit = (e: Object) => {
    e.preventDefault();
    this.setState({ processing: true });
    this.props
      .deleteAnalysis(this.props.project, this.props.analysis.key)
      .then(this.stopProcessingAndClose, this.stopProcessing);
  };

  renderModal() {
    return (
      <Modal
        isOpen={true}
        contentLabel="modal form"
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.closeForm}>

        <header className="modal-head">
          <h2>{translate('project_activity.delete_analysis')}</h2>
        </header>

        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            {translate('project_activity.delete_analysis.question')}
          </div>

          <footer className="modal-foot">
            {this.state.processing
              ? <i className="spinner" />
              : <div>
                  <button type="submit" className="button-red">{translate('delete')}</button>
                  <button type="reset" className="button-link" onClick={this.closeForm}>
                    {translate('cancel')}
                  </button>
                </div>}
          </footer>
        </form>

      </Modal>
    );
  }

  render() {
    return (
      <button className="js-delete-analysis button-small button-red" onClick={this.openForm}>
        {translate('delete')}
        {this.state.open && this.renderModal()}
      </button>
    );
  }
}

const mapStateToProps = null;

const mapDispatchToProps = { deleteAnalysis };

export default connect(mapStateToProps, mapDispatchToProps)(RemoveAnalysisForm);
