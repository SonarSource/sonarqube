/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { get, save } from 'sonar-ui-common/helpers/storage';
import { isSonarCloud } from '../../../../helpers/system';
import CreateSonarPropertiesStep from '../steps/CreateSonarPropertiesStep';
import EditTravisYmlStep from '../steps/EditTravisYmlStep';
import EncryptYourTokenStep from '../steps/EncryptYourTokenStep';
import { PROJECT_STEP_PROGRESS, TutorialProps } from '../utils';

enum Steps {
  ENCRYPT_TOKEN = 1,
  EDIT_TRAVIS_YML = 2,
  CREATE_SONAR_PROPERTIES = 3
}

export default function ConfigureWithTravis({
  component,
  currentUser,
  onDone,
  setToken,
  token
}: TutorialProps) {
  const [build, setBuild] = React.useState<string | undefined>(undefined);
  const [step, setStep] = React.useState<Steps>(Steps.ENCRYPT_TOKEN);
  const [hasStepAfterTravisYml, setHasStepAfterTravilYml] = React.useState<boolean>(false);

  React.useEffect(() => {
    const value = get(PROJECT_STEP_PROGRESS, component.key);
    if (value) {
      try {
        const data = JSON.parse(value);
        setBuild(data.build);
        setStep(data.step);
        setHasStepAfterTravilYml(data.hasStepAfterTravisYml);
      } catch (e) {
        // Let's start from scratch
      }
    }
  }, [component.key]);

  const saveAndFinish = () => {
    save(
      PROJECT_STEP_PROGRESS,
      JSON.stringify({
        build,
        hasStepAfterTravisYml,
        step
      }),
      component.key
    );

    onDone();
  };

  return (
    <>
      <h1 className="spacer-bottom spacer-top">
        {translate('onboarding.analysis.with.travis.title')}
      </h1>

      <EncryptYourTokenStep
        component={component}
        currentUser={currentUser}
        onContinue={() => setStep(Steps.EDIT_TRAVIS_YML)}
        onOpen={() => setStep(Steps.ENCRYPT_TOKEN)}
        open={step === Steps.ENCRYPT_TOKEN}
        setToken={setToken}
        stepNumber={1}
        token={token}
      />

      <EditTravisYmlStep
        buildCallback={setBuild}
        buildType={build}
        component={component}
        finished={step >= 2}
        hasStepAfter={setHasStepAfterTravilYml}
        onContinue={() =>
          hasStepAfterTravisYml ? setStep(Steps.CREATE_SONAR_PROPERTIES) : saveAndFinish()
        }
        onOpen={() => setStep(Steps.EDIT_TRAVIS_YML)}
        open={step === Steps.EDIT_TRAVIS_YML}
        organization={isSonarCloud() ? component.organization : undefined}
        stepNumber={2}
        token={token}
      />

      {hasStepAfterTravisYml && (
        <CreateSonarPropertiesStep
          component={component}
          finished={step >= 3}
          onContinue={saveAndFinish}
          onOpen={() => setStep(Steps.CREATE_SONAR_PROPERTIES)}
          open={step === Steps.CREATE_SONAR_PROPERTIES}
          stepNumber={3}
        />
      )}
    </>
  );
}
