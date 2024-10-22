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

import { Button, ButtonVariety, IconCheck, LinkStandalone } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useSearchParams } from 'react-router-dom';
import {
  Card,
  CardSeparator,
  ClipboardButton,
  InputField,
  ListItem,
  Note,
  OrderedList,
  Title,
} from '~design-system';
import { Image } from '~sonar-aligned/components/common/Image';
import { whenLoggedIn } from '../../components/hoc/whenLoggedIn';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { generateSonarLintUserToken, portIsValid, sendUserToken } from '../../helpers/sonarlint';
import { NewUserToken } from '../../types/token';
import { LoggedInUser } from '../../types/users';

enum Status {
  request,
  tokenError,
  tokenCreated,
  tokenSent,
}

interface Props {
  currentUser: LoggedInUser;
}

export function SonarLintConnection({ currentUser }: Readonly<Props>) {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = React.useState(Status.request);
  const [newToken, setNewToken] = React.useState<NewUserToken | undefined>(undefined);

  const port = parseInt(searchParams.get('port') ?? '0', 10);
  const ideName = searchParams.get('ideName') ?? translate('sonarlint-connection.unspecified-ide');

  const { login } = currentUser;

  const authorize = React.useCallback(async () => {
    const token = await generateSonarLintUserToken({ ideName, login }).catch(() => undefined);

    if (!token) {
      setStatus(Status.tokenError);
      return;
    }

    setNewToken(token);

    if (!portIsValid(port)) {
      setStatus(Status.tokenCreated);
      return;
    }

    try {
      await sendUserToken(port, token);
      setStatus(Status.tokenSent);
    } catch (_) {
      setStatus(Status.tokenCreated);
    }
  }, [port, ideName, login]);

  return (
    <Card className="sw-mt-[10vh] sw-mx-auto sw-w-[650px] sw-text-center">
      {status === Status.request && (
        <>
          <Title>{translate('sonarlint-connection.request.title')}</Title>
          <Image
            alt="sonarlint-connection-request"
            className="sw-my-4"
            src="/images/SonarLint-connection-request.png"
          />
          <p className="sw-my-4">
            {translateWithParameters('sonarlint-connection.request.description', ideName)}
          </p>
          <p className="sw-mb-10">{translate('sonarlint-connection.request.description2')}</p>

          <Button
            prefix={<IconCheck className="sw-mr-1" />}
            onClick={authorize}
            variety={ButtonVariety.Primary}
          >
            {translate('sonarlint-connection.request.action')}
          </Button>
        </>
      )}

      {status === Status.tokenError && (
        <>
          <Image alt="sonarlint-token-error" className="sw-my-4 sw-pt-2" src="/images/cross.svg" />
          <Title>{translate('sonarlint-connection.token-error.title')}</Title>
          <p className="sw-my-4">{translate('sonarlint-connection.token-error.description')}</p>
          <p className="sw-mb-4">
            <FormattedMessage
              id="sonarlint-connection.token-error.description2"
              defaultMessage={translate('sonarlint-connection.token-error.description2')}
              values={{
                link: (
                  <LinkStandalone to="/account/security">
                    {translate('sonarlint-connection.token-error.description2.link')}
                  </LinkStandalone>
                ),
              }}
            />
          </p>
        </>
      )}

      {status === Status.tokenCreated && newToken && (
        <>
          <Image
            alt="sonarlint-connection-error"
            className="sw-my-4 sw-pt-2"
            src="/images/check.svg"
          />
          <Title>{translate('sonarlint-connection.connection-error.title')}</Title>
          <p className="sw-my-6">
            {translate('sonarlint-connection.connection-error.description')}
          </p>
          <div className="sw-flex sw-items-center">
            <Note className="sw-w-abs-150 sw-text-start">
              {translate('sonarlint-connection.connection-error.token-name')}
            </Note>
            {newToken.name}
          </div>
          <CardSeparator className="sw-my-3" />
          <div className="sw-flex sw-items-center">
            <Note className="sw-min-w-abs-150 sw-text-start">
              {translate('sonarlint-connection.connection-error.token-value')}
            </Note>
            <InputField className="sw-cursor-text" disabled size="full" value={newToken.token} />
            <ClipboardButton className="sw-ml-2" copyValue={newToken.token} />
          </div>
          <div className="sw-mt-10">
            <strong>{translate('sonarlint-connection.connection-error.next-steps')}</strong>
          </div>
          <OrderedList className="sw-list-inside sw-mb-4">
            <ListItem>{translate('sonarlint-connection.connection-error.step1')}</ListItem>
            <ListItem>{translate('sonarlint-connection.connection-error.step2')}</ListItem>
          </OrderedList>
        </>
      )}

      {status === Status.tokenSent && newToken && (
        <>
          <Title>{translate('sonarlint-connection.success.title')}</Title>
          <Image
            alt="sonarlint-connection-success"
            className="sw-mb-4"
            src="/images/SonarLint-connection-ok.png"
          />
          <p className="sw-my-4">
            {translateWithParameters('sonarlint-connection.success.description', newToken.name)}
          </p>
          <div className="sw-mt-10">
            <strong>{translate('sonarlint-connection.success.last-step')}</strong>
          </div>
          <div className="sw-my-4">{translate('sonarlint-connection.success.step')}</div>
        </>
      )}
    </Card>
  );
}

export default whenLoggedIn(SonarLintConnection);
