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
import { max } from 'lodash';
import { Fragment, useCallback, useEffect, useState } from 'react';

import {
  ClipboardIconButton,
  CodeEllipsisDirection,
  CodeEllipsisIcon,
  LineCodeEllipsisStyled,
  SonarCodeColorizer,
  themeColor,
} from '~design-system';
import { OpenFixInIde } from '../../apps/issues/components/OpenFixInIde';
import { IssueSourceViewerHeader } from '../../apps/issues/crossComponentSourceViewer/IssueSourceViewerHeader';
import { translate } from '../../helpers/l10n';
import { useComponentForSourceViewer } from '../../queries/component';
import {
  DisplayedLine,
  LineTypeEnum,
  useUnifiedSuggestionsQuery,
} from '../../queries/fix-suggestions';
import { BranchLike } from '../../types/branch-like';
import { Issue } from '../../types/types';
import { IssueSuggestionLine } from './IssueSuggestionLine';

interface Props {
  branchLike?: BranchLike;
  issue: Issue;
  language?: string;
}

const EXPAND_SIZE = 10;
const BUFFER_CODE = 3;

export function IssueSuggestionFileSnippet({ branchLike, issue, language }: Readonly<Props>) {
  const [displayedLine, setDisplayedLine] = useState<DisplayedLine[]>([]);

  const { data: suggestion } = useUnifiedSuggestionsQuery(issue);

  const { data: sourceViewerFile } = useComponentForSourceViewer(issue.component, branchLike);

  useEffect(() => {
    if (suggestion !== undefined) {
      setDisplayedLine(
        suggestion.unifiedLines.filter((line, index) => {
          if (line.type !== LineTypeEnum.CODE) {
            return true;
          }
          return suggestion.unifiedLines
            .slice(max([index - BUFFER_CODE, 0]), index + BUFFER_CODE + 1)
            .some((line) => line.type !== LineTypeEnum.CODE);
        }),
      );
    }
  }, [suggestion]);

  const handleExpand = useCallback(
    (index: number | undefined, at: number | undefined, to: number) => {
      if (suggestion !== undefined) {
        setDisplayedLine((current) => {
          return [
            ...current.slice(0, index),
            ...suggestion.unifiedLines.filter(
              (line) => at !== undefined && at <= line.lineBefore && line.lineBefore < to,
            ),
            ...current.slice(index),
          ];
        });
      }
    },
    [suggestion],
  );

  if (suggestion === undefined) {
    return null;
  }

  return (
    <div>
      {sourceViewerFile && (
        <IssueSourceViewerHeader
          issueKey={issue.key}
          sourceViewerFile={sourceViewerFile}
          shouldShowOpenInIde={false}
          shouldShowViewAllIssues={false}
          secondaryActions={<OpenFixInIde aiSuggestion={suggestion} issue={issue} />}
        />
      )}
      <SourceFileWrapper className="js-source-file sw-mb-4">
        <SonarCodeColorizer>
          {displayedLine[0]?.lineBefore !== 1 && (
            <LineCodeEllipsisStyled
              onClick={() =>
                handleExpand(
                  0,
                  max([displayedLine[0].lineBefore - EXPAND_SIZE, 0]),
                  displayedLine[0].lineBefore,
                )
              }
              style={{ borderTop: 'none' }}
            >
              <CodeEllipsisIcon direction={CodeEllipsisDirection.Up} />
            </LineCodeEllipsisStyled>
          )}
          {displayedLine.map((line, index) => (
            <Fragment key={`${line.lineBefore} -> ${line.lineAfter} `}>
              {displayedLine[index - 1] !== undefined &&
                displayedLine[index - 1].lineBefore !== -1 &&
                line.lineBefore !== -1 &&
                displayedLine[index - 1].lineBefore !== line.lineBefore - 1 && (
                  <>
                    {line.lineBefore - displayedLine[index - 1].lineBefore > EXPAND_SIZE ? (
                      <>
                        <LineCodeEllipsisStyled
                          onClick={() =>
                            handleExpand(
                              index,
                              displayedLine[index - 1].lineBefore + 1,
                              displayedLine[index - 1].lineBefore + EXPAND_SIZE + 1,
                            )
                          }
                        >
                          <CodeEllipsisIcon direction={CodeEllipsisDirection.Down} />
                        </LineCodeEllipsisStyled>
                        <LineCodeEllipsisStyled
                          onClick={() =>
                            handleExpand(index, line.lineBefore - EXPAND_SIZE, line.lineBefore)
                          }
                          style={{ borderTop: 'none' }}
                        >
                          <CodeEllipsisIcon direction={CodeEllipsisDirection.Up} />
                        </LineCodeEllipsisStyled>
                      </>
                    ) : (
                      <LineCodeEllipsisStyled
                        onClick={() =>
                          handleExpand(
                            index,
                            displayedLine[index - 1].lineBefore + 1,
                            line.lineBefore,
                          )
                        }
                      >
                        <CodeEllipsisIcon direction={CodeEllipsisDirection.Middle} />
                      </LineCodeEllipsisStyled>
                    )}
                  </>
                )}
              <div className="sw-relative">
                {line.copy !== undefined && (
                  <StyledClipboardIconButton
                    aria-label={translate('component_viewer.copy_path_to_clipboard')}
                    copyValue={line.copy}
                  />
                )}
                <IssueSuggestionLine
                  language={language}
                  line={line.code}
                  lineAfter={line.lineAfter}
                  lineBefore={line.lineBefore}
                  type={line.type}
                />
              </div>
            </Fragment>
          ))}

          {displayedLine[displayedLine.length - 1]?.lineBefore !==
            suggestion.unifiedLines[suggestion.unifiedLines.length - 1]?.lineBefore && (
            <LineCodeEllipsisStyled
              onClick={() =>
                handleExpand(
                  displayedLine.length,
                  displayedLine[displayedLine.length - 1].lineBefore + 1,
                  displayedLine[displayedLine.length - 1].lineBefore + EXPAND_SIZE + 1,
                )
              }
              style={{ borderBottom: 'none' }}
            >
              <CodeEllipsisIcon direction={CodeEllipsisDirection.Down} />
            </LineCodeEllipsisStyled>
          )}
        </SonarCodeColorizer>
      </SourceFileWrapper>
      <p className="sw-mt-4">{suggestion.explanation}</p>
    </div>
  );
}

const StyledClipboardIconButton = styled(ClipboardIconButton)`
  position: absolute;
  right: 4px;
  top: -4px;
  z-index: 9;
`;

const SourceFileWrapper = styled.div`
  border: 1px solid ${themeColor('codeLineBorder')};
`;
