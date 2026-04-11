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
import * as React from 'react';
import { FlagMessage, Spinner } from '~design-system';
import { getCodefixFixedFile } from '../../api/ai-codefix';
import { getBranchLikeDisplayName } from '../../helpers/branch-like';
import { translate } from '../../helpers/l10n';
import { useRawSourceQuery } from '../../queries/sources';
import { getBranchLikeQuery } from '../../sonar-aligned/helpers/branch-like';
import { BranchLike } from '../../types/branch-like';
import { Issue, SourceViewerFile } from '../../types/types';
import { FixDiffHeader } from './FixDiffHeader';
import { FixDiffTable } from './FixDiffTable';
import { DiffSourceLine } from './FixDiffTypes';
import { UseFixDiffSnippetRange } from './UseFixDiffSnippetRange';
import { UseFixDiffSourceLines } from './UseFixDiffSourceLines';

interface FixDiffTabProps {
  branchLike?: BranchLike;
  issue: Issue;
}

const EXPAND_BY_LINES = 50;

export function FixDiffTab({ branchLike, issue }: Readonly<FixDiffTabProps>) {
  const branchParams = React.useMemo(() => getBranchLikeQuery(branchLike), [branchLike]);

  const {
      data: originalSource,
      isLoading: isSourceLoading,
      isError: isSourceError,
    } = useRawSourceQuery({ ...branchParams, key: issue.component }, { enabled: !!issue.component });

  const fixedFileQuery = useQuery({
    queryKey: ['codefix-fixed-file', issue.key],
    queryFn: () => getCodefixFixedFile(issue.key),
    enabled: Boolean(issue.key),
    retry: (_, error) => {
      const res = error as unknown as Response;
      return res?.status !== 401 && res?.status !== 403;
    },
  });

  const fixedFileData = fixedFileQuery.data;
  const mergedSource = fixedFileData?.fixedFileContent;
  const jobId = fixedFileData?.jobId;

  const hasChanges = React.useMemo(() => {
    if (!originalSource || !mergedSource) return false;
    // Check if there are any differences
    return originalSource !== mergedSource;
  }, [originalSource, mergedSource]);

  // Get language from file extension
  const language = React.useMemo(() => {
    if (!issue.component) {
      return 'plaintext';
    }
    const extension = issue.component.split('.').pop()?.toLowerCase() || '';
    const languageMap: { [key: string]: string } = {
      java: 'java',
      js: 'javascript',
      jsx: 'javascript',
      ts: 'typescript',
      tsx: 'typescript',
      py: 'python',
      cs: 'csharp',
      cpp: 'cpp',
      cc: 'cpp',
      cxx: 'cpp',
      c: 'c',
      go: 'go',
      kt: 'kotlin',
      swift: 'swift',
      php: 'php',
      rb: 'ruby',
      scala: 'scala',
      xml: 'xml',
      json: 'json',
      yaml: 'yaml',
      yml: 'yaml',
      sql: 'sql',
      cls: 'apex',
    };
    return languageMap[extension] || 'plaintext';
  }, [issue.component]);

  // Generate source lines from diff (original vs fixed file)
  const sourceLines = UseFixDiffSourceLines({
    originalSource,
    mergedSource,
    language,
  });

  const { snippetRange, setSnippetRange, minLineNumber, maxLineNumber } = UseFixDiffSnippetRange({
    sourceLines,
    issueContext: undefined,
  });

  const displayedLines = React.useMemo(() => {
    return sourceLines.filter((line) => {
      const diffLine = line as DiffSourceLine;
      const originalNum = diffLine.originalLineNumber;
      const modifiedNum = diffLine.modifiedLineNumber;
      const originalInRange =
        originalNum !== undefined &&
        originalNum >= snippetRange.start &&
        originalNum <= snippetRange.end;
      const modifiedInRange =
        modifiedNum !== undefined &&
        modifiedNum >= snippetRange.start &&
        modifiedNum <= snippetRange.end;
      return originalInRange || modifiedInRange;
    });
  }, [sourceLines, snippetRange]);

  const firstDisplayedLineNumber = React.useMemo(() => {
    if (displayedLines.length === 0) return 0;
    const firstLine = displayedLines[0] as DiffSourceLine;
    return firstLine.originalLineNumber ?? firstLine.modifiedLineNumber ?? firstLine.line ?? 0;
  }, [displayedLines]);

  const lastDisplayedLineNumber = React.useMemo(() => {
    if (displayedLines.length === 0) return 0;
    const lastLine = displayedLines[displayedLines.length - 1] as DiffSourceLine;
    return lastLine.modifiedLineNumber ?? lastLine.originalLineNumber ?? lastLine.line ?? 0;
  }, [displayedLines]);

  const expandUp = React.useCallback(() => {
    setSnippetRange((current) => ({
      start: Math.max(minLineNumber, current.start - EXPAND_BY_LINES),
      end: current.end,
    }));
  }, [setSnippetRange, minLineNumber]);

  const expandDown = React.useCallback(() => {
    setSnippetRange((current) => ({
      start: current.start,
      end: Math.min(maxLineNumber, current.end + EXPAND_BY_LINES),
    }));
  }, [setSnippetRange, maxLineNumber]);

  const filePath = issue.componentLongName || issue.component || '';
  const branchDisplayName = branchLike ? getBranchLikeDisplayName(branchLike) : 'MainAnalysis';

  if (isSourceLoading || fixedFileQuery.isLoading) {
    return (
      <div className="sw-flex sw-justify-center sw-py-8">
        <Spinner ariaLabel={translate('code_viewer.loading')} />
      </div>
    );
  }

  if (isSourceError || fixedFileQuery.isError) {
    return (
      <FlagMessage variant="warning">
        {translate('issues.code_fix.not_able_to_generate_fix')}
      </FlagMessage>
    );
  }

  if (!originalSource || !mergedSource || !hasChanges) {
    return <FlagMessage variant="info">{translate('issue.tabs.fix_diff.empty')}</FlagMessage>;
  }

  return (
    <div className="sw-flex sw-flex-col sw-gap-0">
      <FixDiffHeader
        filePath={filePath}
        branchDisplayName={branchDisplayName}
        projectKey={issue.projectKey}
        projectName={issue.projectName}
        branchLike={branchLike}
        issueKey={issue.key}
        jobId={jobId}
      />
      <FixDiffTable
        branchLike={branchLike}
        displayedLines={displayedLines}
        file={{
          key: issue.component,
          longName: issue.component,
          measures: { lines: String(sourceLines.length) },
        } as SourceViewerFile}
        firstDisplayedLineNumber={firstDisplayedLineNumber}
        lastDisplayedLineNumber={lastDisplayedLineNumber}
        minLineNumber={minLineNumber}
        maxLineNumber={maxLineNumber}
        onExpandUp={expandUp}
        onExpandDown={expandDown}
      />
    </div>
  );
}
