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

import { DropdownMenu } from '@sonarsource/echoes-react';
import { addGlobalErrorMessage, addGlobalSuccessMessage, ButtonSecondary } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import {
  generateSonarLintUserToken,
  openFixOrIssueInSonarLint,
  probeSonarLintServers,
} from '../../../helpers/sonarlint';
import { BranchLike } from '../../../types/branch-like';
import { Ide } from '../../../types/sonarlint';
import { NewUserToken } from '../../../types/token';
import { UserBase } from '../../../types/users';

export interface Props {
  branchLike?: BranchLike;
  issueKey: string;
  login: UserBase['login'];
  projectKey: string;
  pullRequestID?: string;
}

const showError = () =>
  addGlobalErrorMessage(
    <FormattedMessage
      id="issues.open_in_ide.failure"
      values={{
        link: (
          <DocumentationLink to={DocLink.SonarLintConnectedMode}>
            {translate('sonarlint-connected-mode-doc')}
          </DocumentationLink>
        ),
      }}
    />,
  );

const showSuccess = () => addGlobalSuccessMessage(translate('issues.open_in_ide.success'));

const DELAY_AFTER_TOKEN_CREATION = 3000;

export function IssueOpenInIdeButton({ branchLike, issueKey, login, projectKey }: Readonly<Props>) {
  const [isDisabled, setIsDisabled] = React.useState(false);
  const [ides, setIdes] = React.useState<Ide[] | undefined>(undefined);
  const ref = React.useRef<HTMLButtonElement>(null);

  // to give focus back to the trigger button once it is re-rendered as a single button
  const focusTriggerButton = React.useCallback(() => {
    setTimeout(() => {
      ref.current?.focus();
    });
  }, []);

  const openIssue = async (ide: Ide) => {
    setIsDisabled(true);

    let token: NewUserToken | undefined = undefined;

    try {
      if (ide.needsToken) {
        token = await generateSonarLintUserToken({ ideName: ide.ideName, login });
      }

      await openFixOrIssueInSonarLint({
        branchLike,
        calledPort: ide.port,
        issueKey,
        projectKey,
        token,
      });

      showSuccess();
    } catch (e) {
      showError();
    }

    setTimeout(
      () => {
        setIsDisabled(false);
        focusTriggerButton();
      },
      ide.needsToken ? DELAY_AFTER_TOKEN_CREATION : 0,
    );
  };

  const findIDEs = async () => {
    setIdes(undefined);

    const ides = (await probeSonarLintServers()) ?? [];

    if (ides.length === 0) {
      showError();
    } else if (ides.length === 1) {
      openIssue(ides[0]);
    } else {
      setIdes(ides);
    }
  };

  const onClick = ides === undefined ? findIDEs : undefined;

  const triggerButton = (
    <ButtonSecondary
      className="sw-whitespace-nowrap"
      disabled={isDisabled}
      onClick={onClick}
      ref={ref}
    >
      {translate('open_in_ide')}
    </ButtonSecondary>
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
              openIssue(ide);
            }}
          >
            {label}
          </DropdownMenu.ItemButton>
        );
      })}
      onClose={() => {
        setIdes(undefined);
        focusTriggerButton();
      }}
      onOpen={findIDEs}
    >
      {triggerButton}
    </DropdownMenu.Root>
  );
}
