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
import { CardSeparator, CenteredLayout, PageContentFontWrapper } from 'design-system';
import * as React from 'react';
import { useState } from 'react';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { CurrentUserContext } from '../../../app/components/current-user/CurrentUserContext';
import AnalysisMissingInfoMessage from '../../../components/shared/AnalysisMissingInfoMessage';
import { parseDate } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import { areCCTMeasuresComputed, isDiffMetric } from '../../../helpers/measures';
import { CodeScope } from '../../../helpers/urls';
import { useDismissNoticeMutation } from '../../../queries/users';
import { ApplicationPeriod } from '../../../types/application';
import { Branch } from '../../../types/branch-like';
import { Analysis, GraphType, MeasureHistory } from '../../../types/project-activity';
import { QualityGateStatus } from '../../../types/quality-gates';
import { Component, MeasureEnhanced, Metric, Period, QualityGate } from '../../../types/types';
import { NoticeType } from '../../../types/users';
import { AnalysisStatus } from '../components/AnalysisStatus';
import LastAnalysisLabel from '../components/LastAnalysisLabel';
import { Status } from '../utils';
import ActivityPanel from './ActivityPanel';
import BranchMetaTopBar from './BranchMetaTopBar';
import CaycPromotionGuide from './CaycPromotionGuide';
import FirstAnalysisNextStepsNotif from './FirstAnalysisNextStepsNotif';
import MeasuresPanelNoNewCode from './MeasuresPanelNoNewCode';
import NewCodeMeasuresPanel from './NewCodeMeasuresPanel';
import NoCodeWarning from './NoCodeWarning';
import OverallCodeMeasuresPanel from './OverallCodeMeasuresPanel';
import PromotedSection from './PromotedSection';
import QGStatus from './QualityGateStatus';
import ReplayTourGuide from './ReplayTour';
import TabsPanel from './TabsPanel';

export interface BranchOverviewRendererProps {
  analyses?: Analysis[];
  appLeak?: ApplicationPeriod;
  branch?: Branch;
  branchesEnabled?: boolean;
  component: Component;
  detectedCIOnLastAnalysis?: boolean;
  graph?: GraphType;
  loadingHistory?: boolean;
  loadingStatus?: boolean;
  measures?: MeasureEnhanced[];
  measuresHistory?: MeasureHistory[];
  metrics?: Metric[];
  onGraphChange: (graph: GraphType) => void;
  period?: Period;
  projectIsEmpty?: boolean;
  qgStatuses?: QualityGateStatus[];
  qualityGate?: QualityGate;
}

export default function BranchOverviewRenderer(props: BranchOverviewRendererProps) {
  const {
    analyses,
    appLeak,
    branch,
    branchesEnabled,
    component,
    detectedCIOnLastAnalysis,
    graph,
    loadingHistory,
    loadingStatus,
    measures = [],
    measuresHistory = [],
    metrics = [],
    onGraphChange,
    period,
    projectIsEmpty,
    qgStatuses,
    qualityGate,
  } = props;

  const { query } = useLocation();
  const router = useRouter();

  const { currentUser } = React.useContext(CurrentUserContext);

  const { mutateAsync: dismissNotice } = useDismissNoticeMutation();

  const [startTour, setStartTour] = useState(false);
  const [tourCompleted, setTourCompleted] = useState(false);
  const [showReplay, setShowReplay] = useState(false);
  const [dismissedTour, setDismissedTour] = useState(
    currentUser.isLoggedIn &&
      !!currentUser.dismissedNotices[NoticeType.ONBOARDING_CAYC_BRANCH_SUMMARY_GUIDE],
  );

  const tab = query.codeScope === CodeScope.Overall ? CodeScope.Overall : CodeScope.New;
  const leakPeriod = component.qualifier === ComponentQualifier.Application ? appLeak : period;
  const isNewCodeTab = tab === CodeScope.New;
  const hasNewCodeMeasures = measures.some((m) => isDiffMetric(m.metric.key));

  // Check if any potentially missing uncomputed measure is not present
  const isMissingMeasures = !areCCTMeasuresComputed(measures);

  const selectTab = (tab: CodeScope) => {
    router.replace({ query: { ...query, codeScope: tab } });
  };

  React.useEffect(() => {
    // Open Overall tab by default if there are no new measures.
    if (loadingStatus === false && !hasNewCodeMeasures && isNewCodeTab) {
      selectTab(CodeScope.Overall);
    }
    // In this case, we explicitly do NOT want to mark tab as a dependency, as
    // it would prevent the user from selecting it, even if it's empty.
    /* eslint-disable-next-line react-hooks/exhaustive-deps */
  }, [loadingStatus, hasNewCodeMeasures]);

  const analysisMissingInfo = isMissingMeasures && (
    <AnalysisMissingInfoMessage
      qualifier={component.qualifier}
      hide={isPortfolioLike(component.qualifier)}
    />
  );

  const dismissPromotedSection = () => {
    dismissNotice(NoticeType.ONBOARDING_CAYC_BRANCH_SUMMARY_GUIDE);

    setDismissedTour(true);
    setShowReplay(true);
  };

  const closeTour = (action: string) => {
    setStartTour(false);
    if (action === 'skip' && !dismissedTour) {
      dismissPromotedSection();
    }

    if (action === 'close' && !dismissedTour) {
      dismissPromotedSection();
      setTourCompleted(true);
    }
  };

  const startTourGuide = () => {
    if (!isNewCodeTab) {
      selectTab(CodeScope.New);
    }
    setShowReplay(false);
    setStartTour(true);
  };

  const qgStatus = qgStatuses?.map((s) => s.status).includes('ERROR') ? Status.ERROR : Status.OK;

  return (
    <>
      <FirstAnalysisNextStepsNotif
        component={component}
        branchesEnabled={branchesEnabled}
        detectedCIOnLastAnalysis={detectedCIOnLastAnalysis}
      />
      <CenteredLayout>
        <PageContentFontWrapper>
          <CaycPromotionGuide closeTour={closeTour} run={startTour} />
          {showReplay && (
            <ReplayTourGuide
              closeTour={() => setShowReplay(false)}
              run={showReplay}
              tourCompleted={tourCompleted}
            />
          )}
          <div className="overview sw-my-6 sw-body-sm">
            <A11ySkipTarget anchor="overview_main" />

            {projectIsEmpty ? (
              <NoCodeWarning branchLike={branch} component={component} measures={measures} />
            ) : (
              <div>
                {branch && (
                  <>
                    <BranchMetaTopBar
                      branch={branch}
                      component={component}
                      measures={measures}
                      showTakeTheTourButton={
                        dismissedTour && currentUser.isLoggedIn && hasNewCodeMeasures
                      }
                      startTour={startTourGuide}
                    />

                    <CardSeparator />

                    {currentUser.isLoggedIn && hasNewCodeMeasures && (
                      <PromotedSection
                        content={translate('overview.promoted_section.content')}
                        dismissed={dismissedTour ?? false}
                        onDismiss={dismissPromotedSection}
                        onPrimaryButtonClick={startTourGuide}
                        primaryButtonLabel={translate('overview.promoted_section.button_primary')}
                        secondaryButtonLabel={translate(
                          'overview.promoted_section.button_secondary',
                        )}
                        title={translate('overview.promoted_section.title')}
                      />
                    )}
                  </>
                )}
                <AnalysisStatus className="sw-mt-6" component={component} />
                <div
                  data-testid="overview__quality-gate-panel"
                  className="sw-flex sw-justify-between sw-items-start sw-my-6"
                >
                  <QGStatus status={qgStatus} titleSize="extra-large" />
                  <LastAnalysisLabel analysisDate={branch?.analysisDate} />
                </div>
                <div className="sw-flex sw-flex-col sw-mt-6">
                  <TabsPanel
                    analyses={analyses}
                    component={component}
                    loading={loadingStatus}
                    qgStatuses={qgStatuses}
                    isNewCode={isNewCodeTab}
                    onTabSelect={selectTab}
                  >
                    {isNewCodeTab && (
                      <>
                        {hasNewCodeMeasures ? (
                          <NewCodeMeasuresPanel
                            qgStatuses={qgStatuses}
                            branch={branch}
                            component={component}
                            measures={measures}
                            appLeak={appLeak}
                            period={period}
                            loading={loadingStatus}
                            qualityGate={qualityGate}
                          />
                        ) : (
                          <MeasuresPanelNoNewCode
                            branch={branch}
                            component={component}
                            period={period}
                          />
                        )}
                      </>
                    )}

                    {!isNewCodeTab && (
                      <>
                        {analysisMissingInfo}
                        <OverallCodeMeasuresPanel
                          branch={branch}
                          qgStatuses={qgStatuses}
                          component={component}
                          measures={measures}
                          loading={loadingStatus}
                          qualityGate={qualityGate}
                        />
                      </>
                    )}
                  </TabsPanel>

                  <ActivityPanel
                    analyses={analyses}
                    branchLike={branch}
                    component={component}
                    graph={graph}
                    leakPeriodDate={leakPeriod && parseDate(leakPeriod.date)}
                    loading={loadingHistory}
                    measuresHistory={measuresHistory}
                    metrics={metrics}
                    onGraphChange={onGraphChange}
                  />
                </div>
              </div>
            )}
          </div>
        </PageContentFontWrapper>
      </CenteredLayout>
    </>
  );
}
