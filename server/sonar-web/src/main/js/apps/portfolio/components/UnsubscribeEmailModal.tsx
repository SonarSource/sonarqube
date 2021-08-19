/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { unsubscribeFromEmailReport } from '../../../api/component-report';
import { Button, ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import SimpleModal from '../../../components/controls/SimpleModal';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';

interface Props {
  component: T.Component;
  onClose: () => void;
}

interface State {
  success?: boolean;
}

export default class UnsubscribeEmailModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleFormSubmit = async () => {
    const { component } = this.props;

    await unsubscribeFromEmailReport(component.key);

    if (this.mounted) {
      this.setState({ success: true });
    }
  };

  render() {
    const { success } = this.state;
    const header = translate('component_report.unsubscribe');

    return (
      <SimpleModal
        header={header}
        onClose={this.props.onClose}
        onSubmit={this.handleFormSubmit}
        size="small">
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form onSubmit={onFormSubmit}>
            <div className="modal-head">
              <h2>{header}</h2>
            </div>

            <div className="modal-body">
              {success ? (
                <Alert variant="success">{translate('component_report.unsubscribe_success')}</Alert>
              ) : (
                <p>{translate('component_report.unsubscribe.description')}</p>
              )}
            </div>

            <div className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              {success ? (
                <Button onClick={onCloseClick}>{translate('close')}</Button>
              ) : (
                <>
                  <SubmitButton disabled={submitting}>
                    {translate('component_report.unsubscribe')}
                  </SubmitButton>
                  <ResetButtonLink onClick={onCloseClick}>{translate('cancel')}</ResetButtonLink>
                </>
              )}
            </div>
          </form>
        )}
      </SimpleModal>
    );
  }
}
