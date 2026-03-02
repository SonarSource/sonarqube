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

import { useQuery } from '@tanstack/react-query';
import { getCodefixStatus } from '../../../api/ai-codefix';
import { Issue } from '../../../types/types';

const AI_CODE_ASSISTANT_ASSIGNEE = 'ai-code-assistant';

function isAssignedToAiCodeAssistant(issue: Issue): boolean {
  return (
    issue.assignee === AI_CODE_ASSISTANT_ASSIGNEE ||
    issue.assigneeLogin === AI_CODE_ASSISTANT_ASSIGNEE
  );
}

type AiCodefixStatusDisplay = {
  icon: string;
  text: string;
  textClassName: string;
};

function getAiCodefixStatusDisplay(status: string): AiCodefixStatusDisplay {
  const s = (status || '').toUpperCase();
  if (s === 'PENDING' || s === 'IN_PROGRESS') {
    return {
      icon: '/images/spinner.svg',
      text: 'AI Fix in Progress',
      textClassName: 'spinner-text',
    };
  }
  if (s === 'FIX_GENERATED') {
    return {
      icon: '/images/lightning.svg',
      text: 'AI Fix Generated',
      textClassName: 'lightning-text',
    };
  }
  if (s === 'PULL_REQUEST_CREATED') {
    return {
      icon: '/images/pull-request.svg',
      text: 'Pull Request Created',
      textClassName: 'pull-request-text',
    };
  }
  if (s === 'FAILED') {
    return {
      icon: '/images/warning.svg',
      text: 'AI Fix Failed',
      textClassName: 'warning-text',
    };
  }
  return {
    icon: '/images/magic-wand.svg',
    text: 'AI Fix Available',
    textClassName: 'magic-wand-text',
  };
}

/** Cache status for a while to avoid repeated getStatus calls; invalidate after user actions (assign, create PR). */
const CODEFIX_STATUS_STALE_MS = 60_000; // 1 minute
/** When job is in progress, poll so PR created / fix generated shows without user action. */
const CODEFIX_IN_PROGRESS_POLL_MS = 10_000; // 10 seconds

export default function AiCodefixBadge({ issue }: { issue: Issue }) {
  const assignedToAi = isAssignedToAiCodeAssistant(issue);
  const { data: statusData, isLoading, isError } = useQuery({
    queryKey: ['codefix-status', issue.key],
    queryFn: () => getCodefixStatus(issue.key),
    enabled: assignedToAi && Boolean(issue.key),
    staleTime: CODEFIX_STATUS_STALE_MS,
    refetchOnWindowFocus: false,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'PENDING' || status === 'IN_PROGRESS'
        ? CODEFIX_IN_PROGRESS_POLL_MS
        : false;
    },
  });
  console.log(statusData);
  if (!assignedToAi || isLoading || isError || !statusData?.status) {
    return (
      <div className="sparkle-label sw-mr-5">
        <img src="/images/magic-wand.svg" alt="" />
        <span className="status-text magic-wand-text">AI Fix Available</span>
      </div>
    );
  }

  const { icon, text, textClassName } = getAiCodefixStatusDisplay(statusData.status);
  return (
    <div className="codefix-status-label sw-mr-5">
      <img src={icon} alt="" />
      <span className={`status-text ${textClassName}`}>{text}</span>
    </div>
  );
}