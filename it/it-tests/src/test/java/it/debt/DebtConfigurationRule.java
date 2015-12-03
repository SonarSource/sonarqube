package it.debt;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.sonar.orchestrator.Orchestrator;
import java.util.Set;
import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkState;
import static util.ItUtils.setServerProperty;

/**
 * This rule should be used when dealing with technical debt properties, in order to always be sure that the properties are correctly reset between each tests.
 */
public class DebtConfigurationRule extends ExternalResource {

  private static final String HOURS_IN_DAY_PROPERTY = "sonar.technicalDebt.hoursInDay";
  private static final String DEV_COST_PROPERTY = "sonar.technicalDebt.developmentCost";
  private static final String RATING_GRID_PROPERTY = "sonar.technicalDebt.ratingGrid";

  private static final String DEV_COST_LANGUAGE_PROPERTY = "languageSpecificParameters";
  private static final String DEV_COST_LANGUAGE_NAME_PROPERTY = DEV_COST_LANGUAGE_PROPERTY + ".0.language";
  private static final String DEV_COST_LANGUAGE_COST_PROPERTY = DEV_COST_LANGUAGE_PROPERTY + ".0.man_days";

  private static final Joiner COMA_JOINER = Joiner.on(",");

  private static final Set<String> DEV_COST_PROPERTIES = ImmutableSet.of(
    DEV_COST_PROPERTY,
    DEV_COST_LANGUAGE_PROPERTY,
    DEV_COST_LANGUAGE_NAME_PROPERTY,
    DEV_COST_LANGUAGE_COST_PROPERTY,
    RATING_GRID_PROPERTY);

  private final Orchestrator orchestrator;

  private DebtConfigurationRule(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  public static DebtConfigurationRule create(Orchestrator orchestrator) {
    return new DebtConfigurationRule(orchestrator);
  }

  @Override
  protected void before() throws Throwable {
    reset();
  }

  @Override
  protected void after() {
    reset();
  }

  public void reset() {
    resetHoursInDay();
    resetDevelopmentCost();
    resetRatingGrid();
  }

  public DebtConfigurationRule updateHoursInDay(int hoursInDay) {
    setProperty(HOURS_IN_DAY_PROPERTY, Integer.toString(hoursInDay));
    return this;
  }

  public DebtConfigurationRule resetHoursInDay() {
    resetProperty(HOURS_IN_DAY_PROPERTY);
    return this;
  }

  public DebtConfigurationRule updateDevelopmentCost(int developmentCost) {
    setProperty(DEV_COST_PROPERTY, Integer.toString(developmentCost));
    return this;
  }

  public DebtConfigurationRule updateLanguageDevelopmentCost(String language, int developmentCost) {
    setServerProperty(orchestrator, DEV_COST_LANGUAGE_PROPERTY, "0");
    setServerProperty(orchestrator, DEV_COST_LANGUAGE_NAME_PROPERTY, language);
    setServerProperty(orchestrator, DEV_COST_LANGUAGE_COST_PROPERTY, Integer.toString(developmentCost));
    return this;
  }

  public void resetDevelopmentCost() {
    for (String property : DEV_COST_PROPERTIES) {
      resetProperty(property);
    }
  }

  public DebtConfigurationRule updateRatingGrid(Double... ratingGrid) {
    checkState(ratingGrid.length == 4, "Rating grid must contains 4 values");
    setProperty(RATING_GRID_PROPERTY, COMA_JOINER.join(ratingGrid));
    return this;
  }

  public DebtConfigurationRule resetRatingGrid() {
    resetProperty(RATING_GRID_PROPERTY);
    return this;
  }

  private void setProperty(String property, String value) {
    setServerProperty(orchestrator, property, value);
  }

  private void resetProperty(String property) {
    setProperty(property, null);
  }

}
