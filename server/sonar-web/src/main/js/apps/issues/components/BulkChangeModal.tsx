/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { pickBy, sortBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { bulkChangeIssues, searchIssueTags } from '../../../api/issues';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import FormattingTips from '../../../components/common/FormattingTips';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Checkbox from '../../../components/controls/Checkbox';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Modal from '../../../components/controls/Modal';
import Radio from '../../../components/controls/Radio';
import SearchSelect from '../../../components/controls/SearchSelect';
import SelectLegacy from '../../../components/controls/SelectLegacy';
import Tooltip from '../../../components/controls/Tooltip';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { Alert } from '../../../components/ui/Alert';
import Avatar from '../../../components/ui/Avatar';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isLoggedIn, isUserActive } from '../../../helpers/users';
import { Component, CurrentUser, Dict, Issue, IssueType, Paging } from '../../../types/types';
import { searchAssignees } from '../utils';

interface AssigneeOption {
  avatar?: string;
  email?: string;
  label: string;
  value: string;
}

interface TagOption {
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

type AssigneeSelectType = new () => SearchSelect<AssigneeOption>;
const AssigneeSelect = SearchSelect as AssigneeSelectType;

type TagSelectType = new () => SearchSelect<TagOption>;
const TagSelect = SearchSelect as TagSelectType;

export const MAX_PAGE_SIZE = 500;

export default class BulkChangeModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { initialTags: [], issues: [], loading: true, submitting: false };
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
            initialTags: tags.map(tag => ({ label: tag, value: tag })),
            issues,
            loading: false,
            paging
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

  getDefaultAssignee = () => {
    const { currentUser } = this.props;
    const { issues } = this.state;
    const options = [];

    if (isLoggedIn(currentUser)) {
      const canBeAssignedToMe =
        issues.filter(issue => issue.assignee !== currentUser.login).length > 0;
      if (canBeAssignedToMe) {
        options.push({
          avatar: currentUser.avatar,
          label: currentUser.name,
          value: currentUser.login
        });
      }
    }

    const canBeUnassigned = issues.filter(issue => issue.assignee).length > 0;
    if (canBeUnassigned) {
      options.push({ label: translate('unassigned'), value: '' });
    }

    return options;
  };

  handleAssigneeSearch = (query: string) => {
    return searchAssignees(query).then(({ results }) =>
      results.map(r => {
        const userInfo = r.name || r.login;

        return {
          avatar: r.avatar,
          label: isUserActive(r) ? userInfo : translateWithParameters('user.x_deleted', userInfo),
          value: r.login
        };
      })
    );
  };

  handleAssigneeSelect = (assignee: AssigneeOption) => {
    this.setState({ assignee });
  };

  handleTagsSearch = (query: string) => {
    return searchIssueTags({ q: query }).then(tags =>
      tags.map(tag => ({ label: tag, value: tag }))
    );
  };

  handleTagsSelect = (field: 'addTags' | 'removeTags') => (
    options: Array<{ label: string; value: string }>
  ) => {
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

  handleSelectFieldChange = (field: 'severity' | 'type') => (data: { value: string } | null) => {
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
        add_tags: this.state.addTags && this.state.addTags.map(t => t.value).join(),
        assign: this.state.assignee ? this.state.assignee.value : null,
        comment: this.state.comment,
        do_transition: this.state.transition,
        remove_tags: this.state.removeTags && this.state.removeTags.map(t => t.value).join(),
        sendNotifications: this.state.notifications,
        set_severity: this.state.severity,
        set_type: this.state.type
      },
      x => x !== undefined
    );

    const issueKeys = this.state.issues.map(issue => issue.key);

    this.setState({ submitting: true });
    bulkChangeIssues(issueKeys, query).then(
      () => {
        this.setState({ submitting: false });
        this.props.onDone();
      },
      error => {
        this.setState({ submitting: false });
        throwGlobalError(error);
      }
    );
  };

  getAvailableTransitions(issues: Issue[]) {
    const transitions: Dict<number> = {};
    issues.forEach(issue => {
      if (issue.transitions) {
        issue.transitions.forEach(t => {
          if (transitions[t] !== undefined) {
            transitions[t]++;
          } else {
            transitions[t] = 1;
          }
        });
      }
    });
    return sortBy(Object.keys(transitions)).map(transition => ({
      transition,
      count: transitions[transition]
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
          <i className="spinner spacer" />
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
    field: 'addTags' | 'assignee' | 'removeTags' | 'severity' | 'type',
    label: string,
    affected: number | undefined,
    input: React.ReactNode
  ) => (
    <div className="modal-field" id={`issues-bulk-change-${field}`}>
      <label htmlFor={field}>{translate(label)}</label>
      {input}
      {affected !== undefined && this.renderAffected(affected)}
    </div>
  );

  renderAssigneeOption = (option: AssigneeOption) => {
    return (
      <span>
        {option.avatar !== undefined && (
          <Avatar className="spacer-right" hash={option.avatar} name={option.label} size={16} />
        )}
        {option.label}
      </span>
    );
  };

  renderAssigneeField = () => {
    const affected = this.state.issues.filter(hasAction('assign')).length;

    if (affected === 0) {
      return null;
    }

    const input = (
      <AssigneeSelect
        className="input-super-large"
        clearable={true}
        defaultOptions={this.getDefaultAssignee()}
        onSearch={this.handleAssigneeSearch}
        onSelect={this.handleAssigneeSelect}
        renderOption={this.renderAssigneeOption}
        resetOnBlur={false}
        value={this.state.assignee}
      />
    );

    return this.renderField('assignee', 'issue.assign.formlink', affected, input);
  };

  renderTypeField = () => {
    const affected = this.state.issues.filter(hasAction('set_type')).length;

    if (affected === 0) {
      return null;
    }

    const types: IssueType[] = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
    const options = types.map(type => ({ label: translate('issue.type', type), value: type }));

    const optionRenderer = (option: { label: string; value: string }) => (
      <>
        <IssueTypeIcon query={option.value} />
        <span className="little-spacer-left">{option.label}</span>
      </>
    );

    const input = (
      <SelectLegacy
        className="input-super-large"
        clearable={true}
        onChange={this.handleSelectFieldChange('type')}
        optionRenderer={optionRenderer}
        options={options}
        searchable={false}
        value={this.state.type}
        valueRenderer={optionRenderer}
      />
    );

    return this.renderField('type', 'issue.set_type', affected, input);
  };

  renderSeverityField = () => {
    const affected = this.state.issues.filter(hasAction('set_severity')).length;

    if (affected === 0) {
      return null;
    }

    const severities = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];
    const options = severities.map(severity => ({
      label: translate('severity', severity),
      value: severity
    }));

    const input = (
      <SelectLegacy
        className="input-super-large"
        clearable={true}
        onChange={this.handleSelectFieldChange('severity')}
        optionRenderer={(option: { value: string }) => <SeverityHelper severity={option.value} />}
        options={options}
        searchable={false}
        value={this.state.severity}
        valueRenderer={(option: { value: string }) => <SeverityHelper severity={option.value} />}
      />
    );

    return this.renderField('severity', 'issue.set_severity', affected, input);
  };

  renderTagOption = (option: TagOption) => {
    return <span>{option.label}</span>;
  };

  renderTagsField = (field: 'addTags' | 'removeTags', label: string, allowCreate: boolean) => {
    const { initialTags } = this.state;
    const affected = this.state.issues.filter(hasAction('set_tags')).length;

    if (initialTags === undefined || affected === 0) {
      return null;
    }

    const input = (
      <TagSelect
        canCreate={allowCreate}
        className="input-super-large"
        clearable={true}
        defaultOptions={this.state.initialTags}
        minimumQueryLength={0}
        multi={true}
        onMultiSelect={this.handleTagsSelect(field)}
        onSearch={this.handleTagsSearch}
        promptTextCreator={promptCreateTag}
        renderOption={this.renderTagOption}
        resetOnBlur={false}
        value={this.state[field]}
      />
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
        <label>{translate('issue.transition')}</label>
        {transitions.map(transition => (
          <span
            className="bulk-change-radio-button display-flex-center display-flex-space-between"
            key={transition.transition}>
            <Radio
              checked={this.state.transition === transition.transition}
              onCheck={this.handleRadioTransitionChange}
              value={transition.transition}>
              {translate('issue.transition', transition.transition)}
            </Radio>
            {this.renderAffected(transition.count)}
          </span>
        ))}
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
      right={true}>
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
          {this.renderTagsField('addTags', 'issue.add_tags', true)}
          {this.renderTagsField('removeTags', 'issue.remove_tags', false)}
          {this.renderTransitionsField()}
          {this.renderCommentField()}
          {issues.length > 0 && this.renderNotificationsField()}
          {issues.length === 0 && (
            <Alert variant="warning">{translate('issue_bulk_change.no_match')}</Alert>
          )}
        </div>

        <div className="modal-foot">
          {submitting && <i className="spinner spacer-right" />}
          <Tooltip overlay={!canSubmit ? translate('issue_bulk_change.no_change_selected') : null}>
            <SubmitButton
              disabled={!canSubmit || submitting || issues.length === 0}
              id="bulk-change-submit">
              {translate('apply')}
            </SubmitButton>
          </Tooltip>
          <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
        </div>
      </form>
    );
  };

  render() {
    return (
      <Modal contentLabel="modal" onRequestClose={this.props.onClose} size="small">
        {this.state.loading ? this.renderLoading() : this.renderForm()}
      </Modal>
    );
  }
}

function hasAction(action: string) {
  return (issue: Issue) => issue.actions && issue.actions.includes(action);
}

function promptCreateTag(label: string) {
  return `+ ${label}`;
}
