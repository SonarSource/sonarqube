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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import * as React from 'react';
import {
  FlagMessage,
  HtmlFormatter,
  Modal,
  SafeHTMLInjection,
  SanitizeLevel,
  Spinner,
} from '~design-system';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { translate } from '../../../helpers/l10n';
import { useDismissBranchWarningMutation } from '../../../queries/branch';
import { TaskWarning } from '../../../types/tasks';
import { Component } from '../../../types/types';
import { CurrentUser } from '../../../types/users';

interface Props {
  component: Component;
  currentUser: CurrentUser;
  onClose: () => void;
  warnings: TaskWarning[];
}

export function AnalysisWarningsModal(props: Props) {
  const { component, currentUser, warnings } = props;

  const { mutate, isPending, variables } = useDismissBranchWarningMutation(component.key);

  const handleDismissMessage = (messageKey: string) => {
    mutate({ component, key: messageKey });
  };

  const body = (
    <>
      {warnings.map(({ dismissable, key, message }) => (
        <React.Fragment key={key}>
          <div className="sw-flex sw-items-center sw-mt-2">
            <FlagMessage variant="warning">
              <HtmlFormatter>
                <SafeHTMLInjection
                  htmlAsString={message.trim().replace(/\n/g, '<br />')}
                  sanitizeLevel={SanitizeLevel.RESTRICTED}
                />
              </HtmlFormatter>
            </FlagMessage>
          </div>
          <div>
            {dismissable && currentUser.isLoggedIn && (
              <div className="sw-mt-4">
                <Button
                  isDisabled={Boolean(isPending)}
                  onClick={() => {
                    handleDismissMessage(key);
                  }}
                  variety={ButtonVariety.DangerOutline}
                >
                  {translate('dismiss_permanently')}
                </Button>

                <Spinner className="sw-ml-2" loading={isPending && variables?.key === key} />
              </div>
            )}
          </div>
        </React.Fragment>
      ))}
    </>
  );

  return (
    <Modal
      headerTitle={translate('warnings')}
      onClose={props.onClose}
      body={body}
      primaryButton={null}
      secondaryButtonLabel={translate('close')}
    />
  );
}

export default withCurrentUserContext(AnalysisWarningsModal);
