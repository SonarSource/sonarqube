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
import { debounce, pickBy, sortBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { components, OptionProps, SingleValueProps } from 'react-select';
import { bulkChangeIssues, searchIssueTags } from '../../../api/issues';
import FormattingTips from '../../../components/common/FormattingTips';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Checkbox from '../../../components/controls/Checkbox';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Modal from '../../../components/controls/Modal';
import Radio from '../../../components/controls/Radio';
import Select, {
  BasicSelectOption,
  CreatableSelect,
  SearchSelect,
} from '../../../components/controls/Select';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { throwGlobalError } from '../../../helpers/error';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Component, Dict, Issue, IssueType, Paging } from '../../../types/types';
import { CurrentUser } from '../../../types/users';
import AssigneeSelect, { AssigneeOption } from './AssigneeSelect';

const DEBOUNCE_DELAY = 250;

interface TagOption extends BasicSelectOption {
  label: string;
  value: string;
}

interface Props {
  component: Component | undefined;
  currentUser: CurrentUser;
  fetchIssues: (x: {}) => Promise<{ issues: Issue[]; paging: Paging }>;
  onClose: () => void;
  onDone: () => void;
}

interface FormFields {
  addTags?: Array<{ label: string; value: string }>;
  assignee?: AssigneeOption;
  comment?: string;
  notifications?: boolean;
  removeTags?: Array<{ label: string; value: string }>;
  severity?: string;
  transition?: string;
  type?: string;
}

interface State extends FormFields {
  initialTags: Array<{ label: string; value: string }>;
  issues: Issue[];
  // used for initial loading of issues
  loading: boolean;
  paging?: Paging;
  // used when submitting a form
  submitting: boolean;
}

enum InputField {
  addTags = 'addTags',
  assignee = 'assignee',
  removeTags = 'removeTags',
  severity = 'severity',
  type = 'type',
}

export const MAX_PAGE_SIZE = 500;

export default class BulkChangeModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { initialTags: [], issues: [], loading: true, submitting: false };

    this.handleTagsSearch = debounce(this.handleTagsSearch, DEBOUNCE_DELAY);
  }

  componentDidMount() {
    this.mounted = true;

    Promise.all([this.loadIssues(), searchIssueTags({})]).then(
      ([{ issues, paging }, tags]) => {
        if (this.mounted) {
          if (issues.length > MAX_PAGE_SIZE) {
            issues = issues.slice(0, MAX_PAGE_SIZE);
          }

          this.setState({
            initialTags: tags.map((tag) => ({ label: tag, value: tag })),
            issues,
            loading: false,
            paging,
          });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadIssues = () => {
    return this.props.fetchIssues({ additionalFields: 'actions,transitions', ps: MAX_PAGE_SIZE });
  };

  handleAssigneeSelect = (assignee: AssigneeOption) => {
    this.setState({ assignee });
  };

  handleTagsSearch = (query: string, resolve: (option: TagOption[]) => void) => {
    searchIssueTags({ q: query })
      .then((tags) => tags.map((tag) => ({ label: tag, value: tag })))
      .then(resolve)
      .catch(() => resolve([]));
  };

  handleTagsSelect =
    (field: InputField.addTags | InputField.removeTags) => (options: TagOption[]) => {
      this.setState<keyof FormFields>({ [field]: options });
    };

  handleFieldCheck = (field: keyof FormFields) => (checked: boolean) => {
    if (!checked) {
      this.setState<keyof FormFields>({ [field]: undefined });
    } else if (field === 'notifications') {
      this.setState<keyof FormFields>({ [field]: true });
    }
  };

  handleRadioTransitionChange = (transition: string) => {
    this.setState({ transition });
  };

  handleCommentChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) => {
    this.setState({ comment: event.currentTarget.value });
  };

  handleSelectFieldChange = (field: 'severity' | 'type') => (data: BasicSelectOption | null) => {
    if (data) {
      this.setState<keyof FormFields>({ [field]: data.value });
    } else {
      this.setState<keyof FormFields>({ [field]: undefined });
    }
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const query = pickBy(
      {
        add_tags: this.state.addTags && this.state.addTags.map((t) => t.value).join(),
        assign: this.state.assignee ? this.state.assignee.value : null,
        comment: this.state.comment,
        do_transition: this.state.transition,
        remove_tags: this.state.removeTags && this.state.removeTags.map((t) => t.value).join(),
        sendNotifications: this.state.notifications,
        set_severity: this.state.severity,
        set_type: this.state.type,
      },
      (x) => x !== undefined
    );

    const issueKeys = this.state.issues.map((issue) => issue.key);

    this.setState({ submitting: true });
    bulkChangeIssues(issueKeys, query).then(
      () => {
        this.setState({ submitting: false });
        this.props.onDone();
      },
      (error) => {
        this.setState({ submitting: false });
        throwGlobalError(error);
      }
    );
  };

  getAvailableTransitions(issues: Issue[]) {
    const transitions: Dict<number> = {};
    issues.forEach((issue) => {
      if (issue.transitions) {
        issue.transitions.forEach((t) => {
          if (transitions[t] !== undefined) {
            transitions[t]++;
          } else {
            transitions[t] = 1;
          }
        });
      }
    });
    return sortBy(Object.keys(transitions)).map((transition) => ({
      transition,
      count: transitions[transition],
    }));
  }

  canSubmit = () => {
    const { addTags, assignee, removeTags, severity, transition, type } = this.state;

    return Boolean(
      (addTags && addTags.length > 0) ||
        (removeTags && removeTags.length > 0) ||
        assignee ||
        severity ||
        transition ||
        type
    );
  };

  renderLoading = () => (
    <div>
      <div className="modal-head">
        <h2>{translate('bulk_change')}</h2>
      </div>
      <div className="modal-body">
        <div className="text-center">
          <DeferredSpinner
            timeout={0}
            className="spacer"
            ariaLabel={translate('issues.loading_issues')}
          />
        </div>
      </div>
      <div className="modal-foot">
        <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
      </div>
    </div>
  );

  renderAffected = (affected: number) => (
    <div className="pull-right note">
      ({translateWithParameters('issue_bulk_change.x_issues', affected)})
    </div>
  );

  renderField = (
    field: InputField,
    label: string,
    affected: number | undefined,
    input: React.ReactNode
  ) => (
    <div className="modal-field">
      <label htmlFor={`issues-bulk-change-${field}`}>{translate(label)}</label>
      {input}
      {affected !== undefined && this.renderAffected(affected)}
    </div>
  );

  renderAssigneeField = () => {
    const { currentUser } = this.props;
    const { issues } = this.state;
    const affected = this.state.issues.filter(hasAction('assign')).length;
    const field = InputField.assignee;

    if (affected === 0) {
      return null;
    }

    const input = (
      <AssigneeSelect
        inputId={`issues-bulk-change-${field}`}
        currentUser={currentUser}
        issues={issues}
        onAssigneeSelect={this.handleAssigneeSelect}
      />
    );

    return this.renderField(field, 'issue.assign.formlink', affected, input);
  };

  renderTypeField = () => {
    const affected = this.state.issues.filter(hasAction('set_type')).length;
    const field = InputField.type;

    if (affected === 0) {
      return null;
    }

    const types: IssueType[] = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
    const options: BasicSelectOption[] = types.map((type) => ({
      label: translate('issue.type', type),
      value: type,
    }));

    const typeRenderer = (option: BasicSelectOption) => (
      <div className="display-flex-center">
        <IssueTypeIcon query={option.value} />
        <span className="little-spacer-left">{option.label}</span>
      </div>
    );

    const input = (
      <Select
        className="input-super-large"
        inputId={`issues-bulk-change-${field}`}
        isClearable={true}
        isSearchable={false}
        components={{
          Option: (props: OptionProps<BasicSelectOption, false>) => (
            <components.Option {...props}>{typeRenderer(props.data)}</components.Option>
          ),
          SingleValue: (props: SingleValueProps<BasicSelectOption>) => (
            <components.SingleValue {...props}>{typeRenderer(props.data)}</components.SingleValue>
          ),
        }}
        onChange={this.handleSelectFieldChange('type')}
        options={options}
      />
    );

    return this.renderField(field, 'issue.set_type', affected, input);
  };

  renderSeverityField = () => {
    const affected = this.state.issues.filter(hasAction('set_severity')).length;
    const field = InputField.severity;

    if (affected === 0) {
      return null;
    }

    const severities = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];
    const options: BasicSelectOption[] = severities.map((severity) => ({
      label: translate('severity', severity),
      value: severity,
    }));

    const input = (
      <Select
        className="input-super-large"
        inputId={`issues-bulk-change-${field}`}
        isClearable={true}
        isSearchable={false}
        onChange={this.handleSelectFieldChange('severity')}
        components={{
          Option: (props: OptionProps<BasicSelectOption, false>) => (
            <components.Option {...props}>
              {<SeverityHelper className="display-flex-center" severity={props.data.value} />}
            </components.Option>
          ),
          SingleValue: (props: SingleValueProps<BasicSelectOption>) => (
            <components.SingleValue {...props}>
              {<SeverityHelper className="display-flex-center" severity={props.data.value} />}
            </components.SingleValue>
          ),
        }}
        options={options}
      />
    );

    return this.renderField(field, 'issue.set_severity', affected, input);
  };

  renderTagsField = (
    field: InputField.addTags | InputField.removeTags,
    label: string,
    allowCreate: boolean
  ) => {
    const { initialTags } = this.state;
    const affected = this.state.issues.filter(hasAction('set_tags')).length;

    if (initialTags === undefined || affected === 0) {
      return null;
    }

    const props = {
      className: 'input-super-large',
      inputId: `issues-bulk-change-${field}`,
      isClearable: true,
      defaultOptions: this.state.initialTags,
      isMulti: true,
      onChange: this.handleTagsSelect(field),
      loadOptions: this.handleTagsSearch,
    };

    const input = allowCreate ? (
      <CreatableSelect {...props} formatCreateLabel={createTagPrompt} />
    ) : (
      <SearchSelect {...props} />
    );

    return this.renderField(field, label, affected, input);
  };

  renderTransitionsField = () => {
    const transitions = this.getAvailableTransitions(this.state.issues);

    if (transitions.length === 0) {
      return null;
    }

    return (
      <div className="modal-field">
        <fieldset>
          <legend>{translate('issue.transition')}</legend>
          {transitions.map((transition) => (
            <span
              className="bulk-change-radio-button display-flex-center display-flex-space-between"
              key={transition.transition}
            >
              <Radio
                checked={this.state.transition === transition.transition}
                onCheck={this.handleRadioTransitionChange}
                value={transition.transition}
              >
                {translate('issue.transition', transition.transition)}
              </Radio>
              {this.renderAffected(transition.count)}
            </span>
          ))}
        </fieldset>
      </div>
    );
  };

  renderCommentField = () => {
    const affected = this.state.issues.filter(hasAction('comment')).length;

    if (affected === 0) {
      return null;
    }

    return (
      <div className="modal-field">
        <label htmlFor="comment">
          <span className="text-middle">{translate('issue.comment.formlink')}</span>
          <HelpTooltip
            className="spacer-left"
            overlay={translate('issue_bulk_change.comment.help')}
          />
        </label>
        <textarea
          id="comment"
          onChange={this.handleCommentChange}
          rows={4}
          value={this.state.comment || ''}
        />
        <FormattingTips className="modal-field-descriptor text-right" />
      </div>
    );
  };

  renderNotificationsField = () => (
    <Checkbox
      checked={this.state.notifications !== undefined}
      className="display-inline-block spacer-top"
      id="send-notifications"
      onCheck={this.handleFieldCheck('notifications')}
      right={true}
    >
      <strong className="little-spacer-right">{translate('issue.send_notifications')}</strong>
    </Checkbox>
  );

  renderForm = () => {
    const { issues, paging, submitting } = this.state;

    const limitReached = paging && paging.total > MAX_PAGE_SIZE;
    const canSubmit = this.canSubmit();

    return (
      <form id="bulk-change-form" onSubmit={this.handleSubmit}>
        <div className="modal-head">
          <h2>{translateWithParameters('issue_bulk_change.form.title', issues.length)}</h2>
        </div>

        <div className="modal-body modal-container">
          {limitReached && (
            <Alert variant="warning">
              <FormattedMessage
                defaultMessage={translate('issue_bulk_change.max_issues_reached')}
                id="issue_bulk_change.max_issues_reached"
                values={{ max: <strong>{MAX_PAGE_SIZE}</strong> }}
              />
            </Alert>
          )}

          {this.renderAssigneeField()}
          {this.renderTypeField()}
          {this.renderSeverityField()}
          {this.renderTagsField(InputField.addTags, 'issue.add_tags', true)}
          {this.renderTagsField(InputField.removeTags, 'issue.remove_tags', false)}
          {this.renderTransitionsField()}
          {this.renderCommentField()}
          {issues.length > 0 && this.renderNotificationsField()}
          {issues.length === 0 && (
            <Alert variant="warning">{translate('issue_bulk_change.no_match')}</Alert>
          )}
        </div>

        <div className="modal-foot">
          {submitting && <i className="spinner spacer-right" />}
          <SubmitButton
            disabled={!canSubmit || submitting || issues.length === 0}
            id="bulk-change-submit"
          >
            {translate('apply')}
          </SubmitButton>
          <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
        </div>
      </form>
    );
  };

  render() {
    return (
      <Modal onRequestClose={this.props.onClose} size="small">
        {this.state.loading ? this.renderLoading() : this.renderForm()}
      </Modal>
    );
  }
}

function hasAction(action: string) {
  return (issue: Issue) => issue.actions && issue.actions.includes(action);
}

function createTagPrompt(label: string) {
  return translateWithParameters('issue.create_tag_x', label);
}
