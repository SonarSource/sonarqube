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
import * as classnames from 'classnames';
import * as React from 'react';
import BackButton from 'sonar-ui-common/components/controls/BackButton';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { get, remove, save } from 'sonar-ui-common/helpers/storage';
import { getGithubLanguages } from '../../../api/alm-integration';
import { generateToken, getTokens } from '../../../api/user-tokens';
import InstanceMessage from '../../../components/common/InstanceMessage';
import { isBitbucket, isGithub, isVSTS } from '../../../helpers/almIntegrations';
import { isSonarCloud } from '../../../helpers/system';
import AnalyzeTutorialDone from '../components/AnalyzeTutorialDone';
import ProjectAnalysisStep from '../components/ProjectAnalysisStep';
import TokenStep from '../components/TokenStep';
import '../styles.css';
import { getUniqueTokenName } from '../utils';
import './AnalyzeTutorialSonarCloud.css';
import { TutorialSuggestionBitbucket, TutorialSuggestionVSTS } from './AnalyzeTutorialSuggestion';
import ConfigureWithAutoScan from './configurations/ConfigureWithAutoScan';
import ConfigureWithLocalScanner from './configurations/ConfigureWithLocalScanner';
import ConfigureOtherCI from './configurations/ConfigureWithOtherCI';
import ConfigureWithTravis from './configurations/ConfigureWithTravis';
import {
  Alm,
  AlmLanguagesStats,
  alms,
  ALM_KEYS,
  AnalysisMode,
  autoScanMode,
  isAutoScannable,
  modes,
  PROJECT_ONBOARDING_DONE,
  PROJECT_ONBOARDING_MODE_ID,
  PROJECT_STEP_PROGRESS,
  TutorialProps
} from './utils';

interface Props {
  component: T.Component;
  currentUser: T.LoggedInUser;
}

const tutorials: {
  [k: string]: (props: Props & TutorialProps) => JSX.Element;
} = {
  autoscan: ConfigureWithAutoScan,
  manual: ConfigureWithLocalScanner,
  other: ConfigureOtherCI,
  travis: ConfigureWithTravis
};

enum Steps {
  ANALYSIS = 'ANALYSIS',
  TOKEN = 'TOKEN'
}

interface State {
  alm?: Alm;
  almLanguageStats?: AlmLanguagesStats;
  isTutorialDone: boolean;
  mode?: AnalysisMode;
  loading: boolean;
  step: Steps;
  token?: string;
}

export default class AnalyzeTutorialSonarCloud extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      isTutorialDone: get(PROJECT_ONBOARDING_DONE, props.component.key) === 'true',
      step: Steps.TOKEN
    };
  }

  componentDidMount() {
    this.mounted = true;
    const { component, currentUser } = this.props;

    const almKey = (component.alm && component.alm.key) || currentUser.externalProvider;

    if (!almKey) {
      return;
    }

    if (isBitbucket(almKey)) {
      this.configureBitbucket();
    } else if (isGithub(almKey)) {
      this.configureGithub();
    } else if (isVSTS(almKey)) {
      this.configureMicrosoft();
    }

    if (currentUser) {
      getTokens(currentUser.login).then(
        t => {
          this.getNewToken(getUniqueTokenName(t, `Analyze "${component.name}"`));
        },
        () => {}
      );
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    this.setState({ loading: false });
  };

  configureBitbucket = () => {
    this.setState({ alm: alms[ALM_KEYS.BITBUCKET], loading: false });
  };

  configureGithub = () => {
    const { component } = this.props;

    this.setState({
      alm: alms[ALM_KEYS.GITHUB]
    });

    const savedModeId = get(PROJECT_ONBOARDING_MODE_ID, component.key);
    const mode =
      autoScanMode.id === savedModeId ? autoScanMode : modes.find(m => m.id === savedModeId);
    if (mode) {
      this.setState({ mode });
    }

    if (!component.alm) {
      this.stopLoading();
      return;
    }

    const { url } = component.alm;

    if (!url) {
      return;
    }

    getGithubLanguages(url).then(almLanguagesStats => {
      if (this.mounted) {
        this.setState({
          almLanguageStats: almLanguagesStats,
          loading: false
        });
      }
    }, this.stopLoading);
  };

  configureMicrosoft = () => {
    this.setState({ alm: alms[ALM_KEYS.MICROSOFT], loading: false });
  };

  getNewToken = (tokenName: string) => {
    generateToken({ name: tokenName }).then(
      ({ token }: { token: string }) => {
        this.setToken(token);
      },
      () => {}
    );
  };

  setToken = (token: string) => {
    if (this.mounted) {
      this.setState({ token });
    }
  };

  setTutorialDone = (value: boolean) => {
    save(PROJECT_ONBOARDING_DONE, String(value), this.props.component.key);
    this.setState({ isTutorialDone: value });
  };

  spinner = () => (
    <div className="display-flex-justify-center spacer">
      <i className="spinner global-loading-spinner" />
    </div>
  );

  renderBitbucket = () => {
    const { component, currentUser } = this.props;
    const { step, token } = this.state;

    const handleTokenDone = (t: string) => {
      if (this.mounted) {
        this.setState({
          step: Steps.ANALYSIS,
          token: t
        });
      }
    };

    const handleTokenOpen = () => {
      this.setState({ step: Steps.TOKEN });
    };

    return (
      <>
        <TutorialSuggestionBitbucket />

        <TokenStep
          currentUser={currentUser}
          finished={Boolean(token)}
          initialTokenName={`Analyze "${component.name}"`}
          onContinue={handleTokenDone}
          onOpen={handleTokenOpen}
          open={step === Steps.TOKEN}
          stepNumber={1}
        />

        <ProjectAnalysisStep
          component={component}
          displayRowLayout={true}
          open={step === Steps.ANALYSIS}
          organization={isSonarCloud() ? component.organization : undefined}
          stepNumber={2}
          token={token}
        />
      </>
    );
  };

  renderGithub = () => {
    const { almLanguageStats, isTutorialDone, mode, token } = this.state;

    if (isTutorialDone) {
      return <AnalyzeTutorialDone setTutorialDone={this.setTutorialDone} />;
    }

    const { component, currentUser } = this.props;
    const Tutorial = mode && tutorials[mode.id];

    const getClassnames = (item: AnalysisMode) =>
      classnames(`mode-type mode-type-${item.id}`, {
        [`mode-type-selected`]: mode && mode.id === item.id
      });

    const isAutoScanEnabled =
      almLanguageStats && isAutoScannable(almLanguageStats).withAllowedLanguages;

    const setMode = (mode: AnalysisMode | undefined) => {
      if (mode) {
        save(PROJECT_ONBOARDING_MODE_ID, mode.id, component.key);
        remove(PROJECT_STEP_PROGRESS, component.key);
      } else {
        remove(PROJECT_ONBOARDING_MODE_ID, component.key);
      }

      this.setState({ mode });
    };

    if (!mode || !Tutorial) {
      return (
        <div className="page-analysis-container page-analysis-container-sonarcloud">
          <div className="page-analysis big-spacer-top big-spacer-bottom huge-spacer-left huge-spacer-right">
            <div className="page-header big-spacer-bottom">
              <h1 className="big-spacer-bottom">
                {translate('onboarding.project_analysis.header')}
              </h1>
              <p>
                <InstanceMessage message={translate('onboarding.project_analysis.description')} />
              </p>
            </div>

            {isAutoScanEnabled && (
              <div className={`${getClassnames(autoScanMode)} huge-spacer-top huge-spacer-bottom`}>
                <div className="icon">
                  <div className="badge badge-info">BETA</div>
                </div>
                <p>{autoScanMode.name}</p>
                <button
                  className="button big-spacer-top big-spacer-bottom"
                  onClick={() => setMode(autoScanMode)}
                  type="button">
                  {translate('projects.configure_analysis')}
                </button>
              </div>
            )}

            <div className="analysis-modes">
              {modes.map(el => (
                <div className={getClassnames(el)} key={el.id}>
                  <div className="icon" />
                  <div className="name">{el.name}</div>
                  <button
                    className="button big-spacer-top big-spacer-bottom"
                    onClick={() => setMode(el)}
                    type="button">
                    {translate('projects.configure_analysis')}
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className="page-analysis-container page-analysis-container-sonarcloud">
        <BackButton
          onClick={() => setMode(undefined)}
          tooltip={translate('onboarding.tutorial.return_to_list')}>
          {translate('back')}
        </BackButton>

        <Tutorial
          component={component}
          currentUser={currentUser}
          onDone={() => this.setTutorialDone(true)}
          setToken={this.setToken}
          token={token}
        />
      </div>
    );
  };

  renderMicrosoft = () => {
    return <TutorialSuggestionVSTS />;
  };

  render() {
    const { alm, loading } = this.state;

    if (!alm) {
      return null;
    }

    return (
      <DeferredSpinner customSpinner={<this.spinner />} loading={loading}>
        {!loading && (
          <>
            {alm.id === ALM_KEYS.BITBUCKET && this.renderBitbucket()}
            {alm.id === ALM_KEYS.GITHUB && this.renderGithub()}
            {alm.id === ALM_KEYS.MICROSOFT && this.renderMicrosoft()}
          </>
        )}
      </DeferredSpinner>
    );
  }
}
