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
import { Button, ResetButtonLink } from 'sonar-ui-common/components/controls/buttons';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { updateRule } from '../../../api/rules';
import MarkdownTips from '../../../components/common/MarkdownTips';
import RemoveExtendedDescriptionModal from './RemoveExtendedDescriptionModal';

interface Props {
  canWrite: boolean | undefined;
  onChange: (newRuleDetails: T.RuleDetails) => void;
  organization: string | undefined;
  ruleDetails: T.RuleDetails;
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

  handleCancelClick = () => {
    this.setState({ descriptionForm: false });
  };

  handleSaveClick = () => {
    this.updateDescription(this.state.description);
  };

  handleRemoveDescriptionClick = () => {
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
      markdown_note: text,
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

  handleExtendDescriptionClick = () => {
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
          // Safe: defined by rule creator (instance admin?)
          dangerouslySetInnerHTML={{ __html: this.props.ruleDetails.htmlNote }}
        />
      )}
      {this.props.canWrite && (
        <Button
          id="coding-rules-detail-extend-description"
          onClick={this.handleExtendDescriptionClick}>
          {translate('coding_rules.extend_description')}
        </Button>
      )}
    </div>
  );

  renderForm = () => (
    <div className="coding-rules-detail-extend-description-form">
      <table className="width-100">
        <tbody>
          <tr>
            <td colSpan={2}>
              <textarea
                autoFocus={true}
                className="width-100 little-spacer-bottom"
                id="coding-rules-detail-extend-description-text"
                onChange={this.handleDescriptionChange}
                rows={4}
                value={this.state.description}
              />
            </td>
          </tr>
          <tr>
            <td>
              <Button
                disabled={this.state.submitting}
                id="coding-rules-detail-extend-description-submit"
                onClick={this.handleSaveClick}>
                {translate('save')}
              </Button>
              {this.props.ruleDetails.mdNote !== undefined && (
                <>
                  <Button
                    className="button-red spacer-left"
                    disabled={this.state.submitting}
                    id="coding-rules-detail-extend-description-remove"
                    onClick={this.handleRemoveDescriptionClick}>
                    {translate('remove')}
                  </Button>
                  {this.state.removeDescriptionModal && (
                    <RemoveExtendedDescriptionModal
                      onCancel={this.handleCancelRemoving}
                      onSubmit={this.handleConfirmRemoving}
                    />
                  )}
                </>
              )}
              <ResetButtonLink
                className="spacer-left"
                disabled={this.state.submitting}
                id="coding-rules-detail-extend-description-cancel"
                onClick={this.handleCancelClick}>
                {translate('cancel')}
              </ResetButtonLink>
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
    const hasDescription = !ruleDetails.isExternal || ruleDetails.type !== 'UNKNOWN';

    return (
      <div className="js-rule-description">
        {hasDescription ? (
          <div
            className="coding-rules-detail-description rule-desc markdown"
            // Safe: defined by rule creator (instance admin?)
            dangerouslySetInnerHTML={{ __html: ruleDetails.htmlDesc || '' }}
          />
        ) : (
          <div className="coding-rules-detail-description rule-desc markdown">
            {translateWithParameters('issue.external_issue_description', ruleDetails.name)}
          </div>
        )}

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
