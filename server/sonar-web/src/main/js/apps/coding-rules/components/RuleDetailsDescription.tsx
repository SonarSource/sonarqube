/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import {
  ButtonPrimary,
  ButtonSecondary,
  CodeSyntaxHighlighter,
  DangerButtonSecondary,
  InputTextArea,
  Spinner,
} from 'design-system';
import * as React from 'react';
import { updateRule } from '../../../api/rules';
import FormattingTips from '../../../components/common/FormattingTips';
import RuleTabViewer from '../../../components/rules/RuleTabViewer';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { sanitizeString, sanitizeUserInput } from '../../../helpers/sanitize';
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
    removeDescriptionModal: false,
    submitting: false,
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

  handleSaveClick = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
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
      },
    );
  };

  handleExtendDescriptionClick = () => {
    this.setState({
      // set description` to the current `mdNote` each time the form is open
      description: this.props.ruleDetails.mdNote ?? '',
      descriptionForm: true,
    });
  };

  renderExtendedDescription = () => (
    <div id="coding-rules-detail-description-extra">
      {this.props.ruleDetails.htmlNote !== undefined && (
        <CodeSyntaxHighlighter
          className="markdown sw-my-6"
          htmlAsString={sanitizeUserInput(this.props.ruleDetails.htmlNote)}
          language={this.props.ruleDetails.lang}
        />
      )}

      <div className="sw-my-6">
        {this.props.canWrite && (
          <ButtonSecondary onClick={this.handleExtendDescriptionClick}>
            {translate('coding_rules.extend_description')}
          </ButtonSecondary>
        )}
      </div>
    </div>
  );

  renderForm = () => (
    <form
      aria-label={translate('coding_rules.detail.extend_description.form')}
      className="sw-my-6"
      onSubmit={this.handleSaveClick}
    >
      <InputTextArea
        aria-label={translate('coding_rules.extend_description')}
        className="sw-mb-2 sw-resize-y"
        id="coding-rules-detail-extend-description-text"
        size="full"
        onChange={this.handleDescriptionChange}
        rows={4}
        value={this.state.description}
      />

      <div className="sw-flex sw-items-center sw-justify-between">
        <div className="sw-flex sw-items-center">
          <ButtonPrimary
            id="coding-rules-detail-extend-description-submit"
            disabled={this.state.submitting}
            type="submit"
          >
            {translate('save')}
          </ButtonPrimary>

          {this.props.ruleDetails.mdNote !== undefined && (
            <>
              <DangerButtonSecondary
                className="sw-ml-2"
                disabled={this.state.submitting}
                id="coding-rules-detail-extend-description-remove"
                onClick={this.handleRemoveDescriptionClick}
              >
                {translate('remove')}
              </DangerButtonSecondary>
              {this.state.removeDescriptionModal && (
                <RemoveExtendedDescriptionModal
                  onCancel={this.handleCancelRemoving}
                  onSubmit={this.handleConfirmRemoving}
                />
              )}
            </>
          )}

          <ButtonSecondary
            className="sw-ml-2"
            disabled={this.state.submitting}
            id="coding-rules-detail-extend-description-cancel"
            onClick={this.handleCancelClick}
          >
            {translate('cancel')}
          </ButtonSecondary>

          <Spinner className="sw-ml-2" loading={this.state.submitting} />
        </div>

        <FormattingTips />
      </div>
    </form>
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
      (section) => section.key === RuleDescriptionSections.INTRODUCTION,
    )?.content;

    return (
      <div className="js-rule-description">
        {hasDescriptionSection && !defaultSection && (
          <>
            {introductionSection && (
              <CodeSyntaxHighlighter
                className="rule-desc"
                htmlAsString={sanitizeString(introductionSection)}
                language={ruleDetails.lang}
              />
            )}
          </>
        )}

        <RuleTabViewer ruleDetails={ruleDetails} />

        {ruleDetails.isExternal && (
          <div className="coding-rules-detail-description rule-desc markdown">
            {translateWithParameters('issue.external_issue_description', ruleDetails.name)}
          </div>
        )}

        {!ruleDetails.templateKey && (
          <div className="sw-mt-6">
            {!this.state.descriptionForm && this.renderExtendedDescription()}
            {this.state.descriptionForm && this.props.canWrite && this.renderForm()}
          </div>
        )}
      </div>
    );
  }
}
