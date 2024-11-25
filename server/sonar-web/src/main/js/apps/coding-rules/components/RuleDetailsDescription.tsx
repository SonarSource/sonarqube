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
import { updateRule } from '../../../api/rules';
import FormattingTips from '../../../components/common/FormattingTips';
import { Button, ResetButtonLink } from '../../../components/controls/buttons';
import RuleTabViewer from '../../../components/rules/RuleTabViewer';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { SafeHTMLInjection, SanitizeLevel } from '../../../helpers/sanitize';
import { RuleDetails } from '../../../types/types';
import { RuleDescriptionSections } from '../rule';
import RemoveExtendedDescriptionModal from './RemoveExtendedDescriptionModal';

interface Props {
  canWrite: boolean | undefined;
  onChange: (newRuleDetails: RuleDetails) => void;
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
    removeDescriptionModal: false,
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
    }).then(
      (ruleDetails) => {
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
      descriptionForm: true,
    });
  };

  renderExtendedDescription = () => (
    <div id="coding-rules-detail-description-extra">
      {this.props.ruleDetails.htmlNote !== undefined && (
        <SafeHTMLInjection
          htmlAsString={this.props.ruleDetails.htmlNote}
          sanitizeLevel={SanitizeLevel.USER_INPUT}
        >
          <div className="rule-desc spacer-bottom markdown" />
        </SafeHTMLInjection>
      )}

      {this.props.canWrite && (
        <Button
          id="coding-rules-detail-extend-description"
          onClick={this.handleExtendDescriptionClick}
        >
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
                onClick={this.handleSaveClick}
              >
                {translate('save')}
              </Button>
              {this.props.ruleDetails.mdNote !== undefined && (
                <>
                  <Button
                    className="button-red spacer-left"
                    disabled={this.state.submitting}
                    id="coding-rules-detail-extend-description-remove"
                    onClick={this.handleRemoveDescriptionClick}
                  >
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
                onClick={this.handleCancelClick}
              >
                {translate('cancel')}
              </ResetButtonLink>
              {this.state.submitting && <i className="spinner spacer-left" />}
            </td>
            <td className="text-right">
              <FormattingTips />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );

  render() {
    const { ruleDetails } = this.props;
    const hasDescription = !ruleDetails.isExternal || ruleDetails.type !== 'UNKNOWN';

    const hasDescriptionSection =
      hasDescription &&
      ruleDetails.descriptionSections &&
      ruleDetails.descriptionSections.length > 0;

    const defaultSection =
      hasDescriptionSection &&
      ruleDetails.descriptionSections?.length === 1 &&
      ruleDetails.descriptionSections[0].key === RuleDescriptionSections.DEFAULT
        ? ruleDetails.descriptionSections[0]
        : undefined;

    const introductionSection = ruleDetails.descriptionSections?.find(
      (section) => section.key === RuleDescriptionSections.INTRODUCTION
    )?.content;

    return (
      <div className="js-rule-description">
        {defaultSection && (
          <SafeHTMLInjection
            htmlAsString={defaultSection.content}
            sanitizeLevel={SanitizeLevel.FORBID_SVG_MATHML}
          >
            <section
              className="coding-rules-detail-description markdown"
              key={defaultSection.key}
            />
          </SafeHTMLInjection>
        )}

        {hasDescriptionSection && !defaultSection && (
          <>
            {introductionSection && (
              <SafeHTMLInjection
                htmlAsString={introductionSection}
                sanitizeLevel={SanitizeLevel.FORBID_SVG_MATHML}
              >
                <div className="rule-desc" />
              </SafeHTMLInjection>
            )}

            <RuleTabViewer ruleDetails={ruleDetails} />
          </>
        )}

        {ruleDetails.isExternal && (
          <div className="coding-rules-detail-description rule-desc markdown">
            {translateWithParameters('issue.external_issue_description', ruleDetails.name)}
          </div>
        )}

        {!ruleDetails.templateKey && (
          <div className="coding-rules-detail-description coding-rules-detail-description-extra">
            {!this.state.descriptionForm && this.renderExtendedDescription()}
            {this.state.descriptionForm && this.props.canWrite && this.renderForm()}
          </div>
        )}
      </div>
    );
  }
}
