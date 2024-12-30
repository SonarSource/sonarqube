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

import styled from '@emotion/styled';
import { Checkbox, Link, LinkHighlight } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { useEffect, useRef } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { addGlobalSuccessMessage, BasicSeparator, themeBorder } from '~design-system';
import { setIssueSeverity } from '../../../api/issues';
import { useComponent } from '../../../app/components/componentContext/withComponentContext';
import { areMyIssuesSelected, parseQuery, serializeQuery } from '../../../apps/issues/utils';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getIssuesUrl } from '../../../helpers/urls';
import { useLocation } from '../../../sonar-aligned/components/hoc/withRouter';
import { getBranchLikeQuery } from '../../../sonar-aligned/helpers/branch-like';
import { getComponentIssuesUrl } from '../../../sonar-aligned/helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { SoftwareImpactSeverity, SoftwareQuality } from '../../../types/clean-code-taxonomy';
import { IssueActions, IssueSeverity } from '../../../types/issues';
import { Issue } from '../../../types/types';
import SoftwareImpactPillList from '../../shared/SoftwareImpactPillList';
import { updateIssue } from '../actions';
import IssueActionsBar from './IssueActionsBar';
import IssueMetaBar from './IssueMetaBar';
import IssueTags from './IssueTags';
import IssueTitleBar from './IssueTitleBar';

interface Props {
  branchLike?: BranchLike;
  checked?: boolean;
  currentPopup?: string;
  displayWhyIsThisAnIssue?: boolean;
  issue: Issue;
  onAssign: (login: string) => void;
  onChange: (issue: Issue) => void;
  onCheck?: (issue: string) => void;
  onSelect: (issueKey: string) => void;
  selected: boolean;
  togglePopup: (popup: string, show: boolean | void) => void;
}

export default function IssueView(props: Readonly<Props>) {
  const {
    issue,
    branchLike,
    checked,
    currentPopup,
    displayWhyIsThisAnIssue,
    onAssign,
    onChange,
    onSelect,
    togglePopup,
    selected,
    onCheck,
  } = props;
  const intl = useIntl();
  const nodeRef = useRef<HTMLLIElement>(null);
  const { component } = useComponent();
  const location = useLocation();
  const query = parseQuery(location.query);

  const hasCheckbox = onCheck != null;
  const canSetTags = issue.actions.includes(IssueActions.SetTags);
  const canSetSeverity = issue.actions.includes(IssueActions.SetSeverity);

  const handleCheck = () => {
    if (onCheck) {
      onCheck(issue.key);
    }
  };

  const setSeverity = (
    severity: IssueSeverity | SoftwareImpactSeverity,
    quality?: SoftwareQuality,
  ) => {
    const { issue } = props;

    const data = quality
      ? { issue: issue.key, impact: `${quality}=${severity}` }
      : { issue: issue.key, severity: severity as IssueSeverity };

    const severityBefore = quality
      ? issue.impacts.find((impact) => impact.softwareQuality === quality)?.severity
      : issue.severity;

    const linkQuery = {
      ...getBranchLikeQuery(branchLike),
      ...serializeQuery(query),
      myIssues: areMyIssuesSelected(location.query) ? 'true' : undefined,
      open: issue.key,
    };

    return updateIssue(
      onChange,
      setIssueSeverity(data).then((r) => {
        addGlobalSuccessMessage(
          <FormattedMessage
            id="issue.severity.updated_notification"
            values={{
              issueLink: (
                <Link
                  highlight={LinkHighlight.Default}
                  to={
                    component
                      ? getComponentIssuesUrl(component.key, linkQuery)
                      : getIssuesUrl(linkQuery)
                  }
                >
                  {intl.formatMessage(
                    {
                      id: `issue.severity.updated_notification.link.${!quality ? 'standard' : 'mqr'}`,
                    },
                    {
                      type: translate('issue.type', issue.type).toLowerCase(),
                    },
                  )}
                </Link>
              ),
              quality: quality ? translate('software_quality', quality) : undefined,
              before: translate(quality ? 'severity_impact' : 'severity', severityBefore ?? ''),
              after: translate(quality ? 'severity_impact' : 'severity', severity),
            }}
          />,
        );

        return r;
      }),
    );
  };
  useEffect(() => {
    if (selected && nodeRef.current) {
      nodeRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
    }
  }, [selected]);

  return (
    <IssueItem
      onClick={() => onSelect(issue.key)}
      className={classNames('it__issue-item sw-p-3 sw-mb-4 sw-rounded-1 sw-bg-white', {
        selected,
      })}
      ref={nodeRef}
    >
      <section aria-label={issue.message} className="sw-flex sw-gap-3">
        {hasCheckbox && (
          <span className="sw-mt-1/2 sw-ml-1 sw-self-start">
            <Checkbox
              ariaLabel={translateWithParameters('issues.action_select.label', issue.message)}
              checked={checked ?? false}
              onCheck={handleCheck}
              title={translate('issues.action_select')}
            />
          </span>
        )}

        <div className="sw-flex sw-flex-col sw-grow sw-gap-3 sw-min-w-0">
          <IssueTitleBar
            branchLike={branchLike}
            displayWhyIsThisAnIssue={displayWhyIsThisAnIssue}
            issue={issue}
          />

          <div className="sw-mt-1 sw-flex sw-items-start sw-justify-between">
            <SoftwareImpactPillList
              data-guiding-id="issue-2"
              softwareImpacts={issue.impacts}
              onSetSeverity={canSetSeverity ? setSeverity : undefined}
              issueSeverity={issue.severity as IssueSeverity}
              issueType={issue.type}
            />
            <div className="sw-grow-0 sw-whitespace-nowrap">
              <IssueTags
                issue={issue}
                onChange={onChange}
                togglePopup={togglePopup}
                canSetTags={canSetTags}
                open={currentPopup === 'edit-tags' && canSetTags}
              />
            </div>
          </div>

          <BasicSeparator />

          <div className="sw-flex sw-gap-2 sw-flex-nowrap sw-items-center sw-justify-between">
            <IssueActionsBar
              currentPopup={currentPopup}
              issue={issue}
              onAssign={onAssign}
              onChange={onChange}
              togglePopup={togglePopup}
            />
            <IssueMetaBar issue={issue} />
          </div>
        </div>
      </section>
    </IssueItem>
  );
}

const IssueItem = styled.li`
  outline: ${themeBorder('default', 'almCardBorder')};
  outline-offset: -1px;

  &.selected {
    outline: ${themeBorder('heavy', 'primary')};
    outline-offset: -2px;
  }
`;
