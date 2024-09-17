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
import { Button, ButtonVariety, DropdownMenu } from '@sonarsource/echoes-react';
import { addGlobalErrorMessage } from 'design-system/lib';
import React, { useCallback, useState } from 'react';
import { useComponent } from '../../../app/components/componentContext/withComponentContext';
import { useCurrentUser } from '../../../app/components/current-user/CurrentUserContext';
import { translate } from '../../../helpers/l10n';
import { probeSonarLintServers } from '../../../helpers/sonarlint';
import { useCurrentBranchQuery } from '../../../queries/branch';
import { useComponentForSourceViewer } from '../../../queries/component';
import { CodeSuggestion } from '../../../queries/fix-suggestions';
import { useOpenFixOrIssueInIdeMutation } from '../../../queries/sonarlint';
import { Fix, Ide } from '../../../types/sonarlint';
import { Issue } from '../../../types/types';

export interface Props {
  aiSuggestion: CodeSuggestion;
  issue: Issue;
}

const DELAY_AFTER_TOKEN_CREATION = 3000;

export function OpenFixInIde({ aiSuggestion, issue }: Readonly<Props>) {
  const [ides, setIdes] = useState<Ide[] | undefined>(undefined);
  const { component } = useComponent();
  const { data: branchLike, isLoading: isBranchLoading } = useCurrentBranchQuery(component);

  const {
    currentUser: { isLoggedIn },
  } = useCurrentUser();

  const { data: sourceViewerFile } = useComponentForSourceViewer(
    issue.component,
    branchLike,
    !isBranchLoading,
  );
  const { mutateAsync: openFixInIde, isPending } = useOpenFixOrIssueInIdeMutation();

  const closeDropdown = () => {
    setIdes(undefined);
  };

  const openFix = useCallback(
    async (ide: Ide) => {
      closeDropdown();

      const fix: Fix = {
        explanation: aiSuggestion.explanation,
        fileEdit: {
          changes: aiSuggestion.changes.map((change) => ({
            after: change.newCode,
            before: aiSuggestion.unifiedLines
              .filter(
                (line) => line.lineBefore >= change.startLine && line.lineBefore <= change.endLine,
              )
              .map((line) => line.code)
              .join('\n'),
            beforeLineRange: {
              startLine: change.startLine,
              endLine: change.endLine,
            },
          })),
          path: sourceViewerFile?.path ?? '',
        },
        suggestionId: aiSuggestion.suggestionId,
      };

      await openFixInIde({
        branchLike,
        ide,
        fix,
        issue,
      });

      setTimeout(
        () => {
          closeDropdown();
        },
        ide.needsToken ? DELAY_AFTER_TOKEN_CREATION : 0,
      );
    },
    [aiSuggestion, issue, sourceViewerFile, branchLike, openFixInIde],
  );

  const onClick = async () => {
    let IDEs = (await probeSonarLintServers()) ?? [];

    IDEs = IDEs.filter((ide) => ide.capabilities?.canOpenFixSuggestion);

    if (IDEs.length === 0) {
      addGlobalErrorMessage(translate('unable_to_find_ide_with_fix.error'));
    } else if (IDEs.length === 1) {
      openFix(IDEs[0]);
    } else {
      setIdes(IDEs);
    }
  };

  if (!isLoggedIn || branchLike === undefined || sourceViewerFile === undefined) {
    return null;
  }

  const triggerButton = (
    <Button
      className="sw-whitespace-nowrap"
      isDisabled={isPending}
      onClick={onClick}
      variety={ButtonVariety.Default}
    >
      {translate('view_fix_in_ide')}
    </Button>
  );

  return ides === undefined ? (
    triggerButton
  ) : (
    <DropdownMenu.Root
      isOpenOnMount
      items={ides.map((ide) => {
        const { ideName, description } = ide;

        const label = ideName + (description ? ` - ${description}` : '');

        return (
          <DropdownMenu.ItemButton
            key={ide.port}
            onClick={() => {
              openFix(ide);
            }}
          >
            {label}
          </DropdownMenu.ItemButton>
        );
      })}
      onClose={() => {
        setIdes(undefined);
      }}
      onOpen={onClick}
    >
      {triggerButton}
    </DropdownMenu.Root>
  );
}
