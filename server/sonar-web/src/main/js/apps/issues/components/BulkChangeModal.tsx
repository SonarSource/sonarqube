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

import { Checkbox, RadioButtonGroup, Spinner } from '@sonarsource/echoes-react';
import {
  ButtonPrimary,
  FlagMessage,
  FormField,
  Highlight,
  InputTextArea,
  LightLabel,
  Modal,
} from 'design-system';
import { countBy, flattenDeep, pickBy, sortBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { bulkChangeIssues, searchIssueTags } from '../../../api/issues';
import FormattingTips from '../../../components/common/FormattingTips';
import { isTransitionHidden, transitionRequiresComment } from '../../../components/issue/helpers';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { withBranchStatusRefresh } from '../../../queries/branch';
import { IssueTransition } from '../../../types/issues';
import { Issue, Paging } from '../../../types/types';
import AssigneeSelect from './AssigneeSelect';
import TagsSelect from './TagsSelect';

interface Props {
  fetchIssues: (x: {}) => Promise<{ issues: Issue[]; paging: Paging }>;
  needIssueSync?: boolean;
  onClose: () => void;
  onDone: () => void;
  refreshBranchStatus: () => void;
}

interface FormFields {
  addTags?: Array<string>;
  assignee?: string;
  comment?: string;
  notifications?: boolean;
  removeTags?: Array<string>;
  severity?: string;
  transition?: IssueTransition;
  type?: string;
}

interface State extends FormFields {
  initialTags: Array<string>;
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

export class BulkChangeModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { initialTags: [], issues: [], loading: true, submitting: false };
  }

  componentDidMount() {
    const { needIssueSync } = this.props;

    this.mounted = true;

    Promise.all([
      this.loadIssues(),
      needIssueSync ? Promise.resolve([]) : searchIssueTags({}),
    ]).then(
      ([{ issues, paging }, tags]) => {
        if (this.mounted) {
          if (issues.length > MAX_PAGE_SIZE) {
            issues = issues.slice(0, MAX_PAGE_SIZE);
          }

          this.setState({
            initialTags: tags,
            issues,
            loading: false,
            paging,
          });
        }
      },
      () => {},
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadIssues = () => {
    return this.props.fetchIssues({ additionalFields: 'actions,transitions', ps: MAX_PAGE_SIZE });
  };

  handleAssigneeSelect = (assignee: string) => {
    this.setState({ assignee });
  };

  handleTagsSearch = (query: string): Promise<string[]> => {
    return searchIssueTags({ q: query })
      .then((tags) => tags)
      .catch(() => []);
  };

  handleTagsSelect =
    (field: InputField.addTags | InputField.removeTags) => (options: Array<string>) => {
      this.setState<keyof FormFields>({ [field]: options });
    };

  handleFieldCheck = (field: keyof FormFields) => (checked: boolean) => {
    if (!checked) {
      this.setState<keyof FormFields>({ [field]: undefined });
    } else if (field === 'notifications') {
      this.setState<keyof FormFields>({ [field]: true });
    }
  };

  handleRadioTransitionChange = (transition: IssueTransition) => {
    this.setState({ transition });
  };

  handleCommentChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) => {
    this.setState({ comment: event.currentTarget.value });
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const {
      addTags,
      assignee,
      comment,
      issues,
      notifications,
      removeTags,
      severity,
      transition,
      type,
    } = this.state;

    const query = pickBy(
      {
        add_tags: addTags?.join(),
        assign: assignee,
        comment,
        do_transition: transition,
        remove_tags: removeTags?.join(),
        sendNotifications: notifications,
        set_severity: severity,
        set_type: type,
      },
      (x) => x !== undefined,
    );

    const issueKeys = issues.map((issue) => issue.key);

    this.setState({ submitting: true });

    bulkChangeIssues(issueKeys, query).then(
      () => {
        this.setState({ submitting: false });
        this.props.refreshBranchStatus();
        this.props.onDone();
      },
      (error) => {
        this.setState({ submitting: false });
        throwGlobalError(error);
      },
    );
  };

  getAvailableTransitions(issues: Issue[]) {
    const allTransitions = flattenDeep(issues.map((issue) => issue.transitions));
    const countTransitions = countBy<IssueTransition>(allTransitions);

    return sortBy(Object.keys(countTransitions)).map((transition: IssueTransition) => ({
      transition,
      count: countTransitions[transition],
    }));
  }

  canSubmit = () => {
    const { addTags, assignee, removeTags, severity, transition, type } = this.state;

    return Boolean(
      (addTags && addTags.length > 0) ||
        (removeTags && removeTags.length > 0) ||
        assignee !== undefined ||
        severity ||
        transition ||
        type,
    );
  };

  renderField = (
    field: InputField,
    label: string,
    affected: number | undefined,
    input: React.ReactNode,
  ) => (
    <FormField htmlFor={`issues-bulk-change-${field}`} label={translate(label)}>
      <div className="sw-flex sw-items-center sw-justify-between">
        {input}

        {affected !== undefined && (
          <LightLabel>
            ({translateWithParameters('issue_bulk_change.x_issues', affected)})
          </LightLabel>
        )}
      </div>
    </FormField>
  );

  renderAssigneeField = () => {
    const { assignee, issues } = this.state;
    const affected = this.state.issues.filter(hasAction('assign')).length;
    const field = InputField.assignee;

    if (affected === 0) {
      return null;
    }

    return (
      <div className="sw-flex sw-items-center sw-justify-between sw-mb-6">
        <AssigneeSelect
          className="sw-max-w-abs-300"
          inputId={`issues-bulk-change-${field}`}
          issues={issues}
          label={translate('issue.assign.formlink')}
          onAssigneeSelect={this.handleAssigneeSelect}
          selectedAssigneeKey={assignee}
        />
        {affected !== undefined && (
          <LightLabel>
            ({translateWithParameters('issue_bulk_change.x_issues', affected)})
          </LightLabel>
        )}
      </div>
    );
  };

  renderTagsField = (
    field: InputField.addTags | InputField.removeTags,
    label: string,
    allowCreate: boolean,
  ) => {
    const { initialTags } = this.state;
    const tags = this.state[field] ?? [];
    const affected = this.state.issues.filter(hasAction('set_tags')).length;

    if (initialTags === undefined || affected === 0) {
      return null;
    }

    const input = (
      <TagsSelect
        allowCreation={allowCreate}
        inputId={`issues-bulk-change-${field}`}
        onChange={this.handleTagsSelect(field)}
        selectedTags={tags}
        onSearch={this.handleTagsSearch}
      />
    );

    return this.renderField(field, label, affected, input);
  };

  renderTransitionsField = () => {
    const transitions = this.getAvailableTransitions(this.state.issues).filter(
      (transition) => !isTransitionHidden(transition.transition),
    );

    if (transitions.length === 0) {
      return null;
    }

    return (
      <div className="sw-mb-6">
        <fieldset>
          <Highlight as="legend" className="sw-mb-2">
            {translate('issue.change_status')}
          </Highlight>

          <RadioButtonGroup
            ariaLabel="a"
            id="bulk-change-transition"
            options={transitions.map(({ transition, count }) => ({
              label: translate('issue.transition', transition),
              value: transition,
              helpText: translateWithParameters('issue_bulk_change.x_issues', count),
            }))}
            onChange={this.handleRadioTransitionChange}
          />
        </fieldset>
      </div>
    );
  };

  renderCommentField = () => {
    const affectedIssuesCount = this.state.issues.filter(hasAction('comment')).length;
    if (affectedIssuesCount === 0) {
      return null;
    }

    // Selected transition does not require comment
    if (!this.state.transition || !transitionRequiresComment(this.state.transition)) {
      return null;
    }

    return (
      <FormField label={translate('issue_bulk_change.resolution_comment')}>
        <InputTextArea
          aria-label={translate('issue_bulk_change.resolution_comment')}
          onChange={this.handleCommentChange}
          placeholder={translate(
            'issue.transition.comment.placeholder',
            this.state.transition ?? '',
          )}
          rows={5}
          value={this.state.comment}
          size="auto"
          className="sw-resize-y sw-w-full"
        />
        <FormattingTips className="sw-mt-2" />
      </FormField>
    );
  };

  renderNotificationsField = () => (
    <Checkbox
      checked={this.state.notifications !== undefined}
      id="send-notifications"
      label={translate('issue.send_notifications')}
      onCheck={this.handleFieldCheck('notifications')}
    />
  );

  renderForm = () => {
    const { needIssueSync } = this.props;
    const { issues, loading, paging } = this.state;

    const limitReached = paging && paging.total > MAX_PAGE_SIZE;

    return (
      <Spinner isLoading={loading}>
        <form id="bulk-change-form" onSubmit={this.handleSubmit} className="sw-mr-4">
          {limitReached && (
            <FlagMessage className="sw-mb-4" variant="warning">
              <span>
                <FormattedMessage
                  defaultMessage={translate('issue_bulk_change.max_issues_reached')}
                  id="issue_bulk_change.max_issues_reached"
                  values={{ max: <strong>{MAX_PAGE_SIZE}</strong> }}
                />
              </span>
            </FlagMessage>
          )}

          {this.renderAssigneeField()}
          {!needIssueSync && this.renderTagsField(InputField.addTags, 'issue.add_tags', true)}

          {!needIssueSync &&
            this.renderTagsField(InputField.removeTags, 'issue.remove_tags', false)}

          {this.renderTransitionsField()}
          {this.renderCommentField()}
          {issues.length > 0 && this.renderNotificationsField()}

          {issues.length === 0 && (
            <FlagMessage variant="warning">{translate('issue_bulk_change.no_match')}</FlagMessage>
          )}
        </form>
      </Spinner>
    );
  };

  render() {
    const { issues, loading, submitting } = this.state;

    const canSubmit = this.canSubmit();

    return (
      <Modal
        body={this.renderForm()}
        headerTitle={
          loading
            ? translate('bulk_change')
            : translateWithParameters('issue_bulk_change.form.title', issues.length)
        }
        isScrollable
        loading={submitting}
        onClose={this.props.onClose}
        primaryButton={
          <ButtonPrimary
            disabled={!canSubmit || submitting || issues.length === 0}
            form="bulk-change-form"
            id="bulk-change-submit"
            type="submit"
          >
            {translate('apply')}
          </ButtonPrimary>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}

function hasAction(action: string) {
  return (issue: Issue) => issue.actions?.includes(action);
}

export default withBranchStatusRefresh(BulkChangeModal);
