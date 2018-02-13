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
import RemoveExtendedDescriptionModal from './RemoveExtendedDescriptionModal';
import { updateRule } from '../../../api/rules';
import { RuleDetails } from '../../../app/types';
import MarkdownTips from '../../../components/common/MarkdownTips';
import { translate } from '../../../helpers/l10n';

interface Props {
  canWrite: boolean | undefined;
  onChange: (newRuleDetails: RuleDetails) => void;
  organization: string | undefined;
  ruleDetails: RuleDetails;
}

interface State {
  description: string;
  descriptionForm: boolean;
  removeDescriptionModal: boolean;
  submitting: boolean;
}

export default class RuleDetailsDescription extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    description: '',
    descriptionForm: false,
    submitting: false,
    removeDescriptionModal: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDescriptionChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) =>
    this.setState({ description: event.currentTarget.value });

  handleCancelClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ descriptionForm: false });
  };

  handleSaveClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.updateDescription(this.state.description);
  };

  handleRemoveDescriptionClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ removeDescriptionModal: true });
  };

  handleCancelRemoving = () => this.setState({ removeDescriptionModal: false });

  handleConfirmRemoving = () => {
    this.setState({ removeDescriptionModal: false });
    this.updateDescription('');
  };

  updateDescription = (text: string) => {
    this.setState({ submitting: true });

    updateRule({
      key: this.props.ruleDetails.key,
      /* eslint-disable camelcase */
      markdown_note: text,
      /* eslint-enable camelcase*/
      organization: this.props.organization
    }).then(
      ruleDetails => {
        this.props.onChange(ruleDetails);
        if (this.mounted) {
          this.setState({ submitting: false, descriptionForm: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ submitting: false });
        }
      }
    );
  };

  handleExtendDescriptionClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({
      // set description` to the current `mdNote` each time the form is open
      description: this.props.ruleDetails.mdNote || '',
      descriptionForm: true
    });
  };

  renderDescription = () => (
    <div id="coding-rules-detail-description-extra">
      {this.props.ruleDetails.htmlNote !== undefined && (
        <div
          className="rule-desc spacer-bottom markdown"
          dangerouslySetInnerHTML={{ __html: this.props.ruleDetails.htmlNote }}
        />
      )}
      {this.props.canWrite && (
        <button
          id="coding-rules-detail-extend-description"
          onClick={this.handleExtendDescriptionClick}>
          {translate('coding_rules.extend_description')}
        </button>
      )}
    </div>
  );

  renderForm = () => (
    <div className="coding-rules-detail-extend-description-form">
      <table className="width100">
        <tbody>
          <tr>
            <td className="width100" colSpan={2}>
              <textarea
                autoFocus={true}
                id="coding-rules-detail-extend-description-text"
                onChange={this.handleDescriptionChange}
                rows={4}
                style={{ width: '100%', marginBottom: 4 }}
                value={this.state.description}
              />
            </td>
          </tr>
          <tr>
            <td>
              <button
                disabled={this.state.submitting}
                id="coding-rules-detail-extend-description-submit"
                onClick={this.handleSaveClick}>
                {translate('save')}
              </button>
              {this.props.ruleDetails.mdNote !== undefined && (
                <>
                  <button
                    className="button-red spacer-left"
                    disabled={this.state.submitting}
                    id="coding-rules-detail-extend-description-remove"
                    onClick={this.handleRemoveDescriptionClick}>
                    {translate('remove')}
                  </button>
                  {this.state.removeDescriptionModal && (
                    <RemoveExtendedDescriptionModal
                      onCancel={this.handleCancelRemoving}
                      onSubmit={this.handleConfirmRemoving}
                    />
                  )}
                </>
              )}
              <button
                className="spacer-left button-link"
                disabled={this.state.submitting}
                id="coding-rules-detail-extend-description-cancel"
                onClick={this.handleCancelClick}>
                {translate('cancel')}
              </button>
              {this.state.submitting && <i className="spinner spacer-left" />}
            </td>
            <td className="text-right">
              <MarkdownTips />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );

  render() {
    const { ruleDetails } = this.props;

    return (
      <div className="js-rule-description">
        <div
          className="coding-rules-detail-description rule-desc markdown"
          dangerouslySetInnerHTML={{ __html: ruleDetails.htmlDesc || '' }}
        />

        {!ruleDetails.templateKey && (
          <div className="coding-rules-detail-description coding-rules-detail-description-extra">
            {!this.state.descriptionForm && this.renderDescription()}
            {this.state.descriptionForm && this.props.canWrite && this.renderForm()}
          </div>
        )}
      </div>
    );
  }
}
