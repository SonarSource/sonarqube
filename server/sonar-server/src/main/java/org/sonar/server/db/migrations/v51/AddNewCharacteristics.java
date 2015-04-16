/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations.v51;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.Select;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * See http://jira.codehaus.org/browse/SONAR-6187
 *
 * Add a new Characteristic 'Usability' with 2 sub-characteristics 'Accessibility' and 'Ease of Use'
 * and add a new sub-characteristic 'Compliance' for all characteristics.
 *
 * Nothing will be done if there's no characteristics in db, as they're all gonna be created by {@link org.sonar.server.startup.RegisterDebtModel}
 *
 * Before 4.3 the characteristics table contains requirements, then when selecting characteristics we should not forget to exclude them (with a filter on rule_id IS NULL)
 *
 */
public class AddNewCharacteristics extends BaseDataChange {

  private static final Logger LOGGER = LoggerFactory.getLogger(AddNewCharacteristics.class);

  private static final String COMPLIANCE_NAME = "Compliance";
  private static final String COMPLIANCE_KEY_SUFFIX = "_COMPLIANCE";

  private static final String SECURITY_KEY = "SECURITY";
  private static final String USABILITY_KEY = "USABILITY";

  private static final String ERROR_SUFFIX = "Please restore your DB backup, start the previous version of SonarQube " +
    "and update your SQALE model to fix this issue before trying again to run the migration.";

  private final System2 system;

  public AddNewCharacteristics(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    CharacteristicsContext characteristicsContext = new CharacteristicsContext(context, system);

    // On an empty DB, there are no characteristics, they're all gonna be created after in RegisterDebtModel
    if (!characteristicsContext.characteristics().isEmpty()) {
      int usabilityOder = moveCharacteristicsDownToBeAbleToInsertUsability(characteristicsContext);
      createOrUpdateUsabilityCharacteristicAndItsSubCharacteristic(characteristicsContext, usabilityOder);

      createSubCharacteristic(characteristicsContext, "REUSABILITY" + COMPLIANCE_KEY_SUFFIX, "Reusability " + COMPLIANCE_NAME, "REUSABILITY");
      createSubCharacteristic(characteristicsContext, "PORTABILITY" + COMPLIANCE_KEY_SUFFIX, "Portability " + COMPLIANCE_NAME, "PORTABILITY");
      createSubCharacteristic(characteristicsContext, "MAINTAINABILITY" + COMPLIANCE_KEY_SUFFIX, "Maintainability " + COMPLIANCE_NAME, "MAINTAINABILITY");
      createSubCharacteristic(characteristicsContext, SECURITY_KEY + COMPLIANCE_KEY_SUFFIX, "Security " + COMPLIANCE_NAME, SECURITY_KEY);
      createSubCharacteristic(characteristicsContext, "EFFICIENCY" + COMPLIANCE_KEY_SUFFIX, "Efficiency " + COMPLIANCE_NAME, "EFFICIENCY");
      createSubCharacteristic(characteristicsContext, "CHANGEABILITY" + COMPLIANCE_KEY_SUFFIX, "Changeability " + COMPLIANCE_NAME, "CHANGEABILITY");
      createSubCharacteristic(characteristicsContext, "RELIABILITY" + COMPLIANCE_KEY_SUFFIX, "Reliability " + COMPLIANCE_NAME, "RELIABILITY");
      createSubCharacteristic(characteristicsContext, "TESTABILITY" + COMPLIANCE_KEY_SUFFIX, "Testability " + COMPLIANCE_NAME, "TESTABILITY");
    }
  }

  /**
   * If the characteristic 'Security' exists, the new characteristic 'Usability' should be inserted just below it,
   * so every existing characteristics below Security should move down.
   *
   * If the characteristic 'Security' does not exists, the new characteristic 'Usability' should be the first one,
   * so every existing characteristics should move down.
   *
   * If the characteristic 'Usability' is already at the right place, nothing will be done.
   */
  private int moveCharacteristicsDownToBeAbleToInsertUsability(CharacteristicsContext characteristicsContext) throws SQLException {
    Characteristic security = characteristicsContext.findCharacteristicByKey(SECURITY_KEY);
    Characteristic usability = characteristicsContext.findCharacteristicByKey(USABILITY_KEY);

    int usabilityOder = 1;
    int indexToStart = 0;
    if (security != null) {
      indexToStart = characteristicsContext.characteristics().indexOf(security) + 1;
      usabilityOder = security.getOrder() + 1;
    }

    if (usability == null || usability.getOrder() != usabilityOder) {
      // Move root characteristics one step lower
      for (int i = indexToStart; i < characteristicsContext.characteristics().size(); i++) {
        Characteristic characteristic = characteristicsContext.characteristics().get(i);
        if (characteristic.getParentId() == null) {
          characteristicsContext.updateCharacteristicOrder(characteristic.getKey(), characteristic.getOrder() + 1);
        }
      }
    }
    return usabilityOder;
  }

  private void createOrUpdateUsabilityCharacteristicAndItsSubCharacteristic(CharacteristicsContext characteristicsContext, int newUsabilityOrder)
    throws SQLException {
    String usabilityKey = USABILITY_KEY;
    Characteristic usability = characteristicsContext.findCharacteristicByKey(usabilityKey);
    if (usability != null) {
      if (usability.getOrder() != newUsabilityOrder) {
        usability.setOrder(newUsabilityOrder);
        characteristicsContext.updateCharacteristicOrder(usability.getKey(), usability.getOrder());
      }
    } else {
      usability = new Characteristic().setKey(usabilityKey).setName("Usability").setOrder(newUsabilityOrder);
      characteristicsContext.insertCharacteristic(usability);
    }

    createSubCharacteristic(characteristicsContext, "USABILITY_ACCESSIBILITY", "Accessibility", usabilityKey);
    createSubCharacteristic(characteristicsContext, "USABILITY_EASE_OF_USE", "Ease of Use", usabilityKey);
    createSubCharacteristic(characteristicsContext, USABILITY_KEY + COMPLIANCE_KEY_SUFFIX, "Usability " + COMPLIANCE_NAME, usabilityKey);
  }

  private void createSubCharacteristic(CharacteristicsContext characteristicsContext,
    String subCharacteristicKey, String subCharacteristicName, String parentKey) throws SQLException {
    Characteristic parent = characteristicsContext.findCharacteristicByKey(parentKey);
    if (parent != null) {
      Characteristic subCharacteristic = characteristicsContext.findSubCharacteristicByKey(subCharacteristicKey, parent);
      if (subCharacteristic == null) {
        characteristicsContext.insertCharacteristic(new Characteristic().setKey(subCharacteristicKey).setName(subCharacteristicName).setParentId(parent.getId()));
      }
    }
    // If the characteristic parent does not exits, the sub-characteristic is not added
  }

  private static class Characteristic {
    private Integer id;
    private String key;
    private String name;
    private Integer order;
    private Integer parentId;

    public Integer getId() {
      return id;
    }

    public Characteristic setId(Integer id) {
      this.id = id;
      return this;
    }

    public String getKey() {
      return key;
    }

    public Characteristic setKey(String key) {
      this.key = key;
      return this;
    }

    public String getName() {
      return name;
    }

    public Characteristic setName(String name) {
      this.name = name;
      return this;
    }

    /**
     * On a characteristic, the order can never be null
     */
    public Integer getOrder() {
      return parentId == null && order != null ? order : null;
    }

    public Characteristic setOrder(@Nullable Integer order) {
      this.order = order;
      return this;
    }

    @CheckForNull
    public Integer getParentId() {
      return parentId;
    }

    public Characteristic setParentId(@Nullable Integer parentId) {
      this.parentId = parentId;
      return this;
    }
  }

  private static class CharacteristicsContext {
    private final System2 system;
    Context context;
    Date now;
    List<Characteristic> characteristics;

    public CharacteristicsContext(Context context, System2 system) throws SQLException {
      this.context = context;
      this.system = system;
      init();
    }

    private void init() throws SQLException {
      now = new Date(system.now());
      characteristics = selectEnabledCharacteristics();
    }

    public List<Characteristic> characteristics() {
      return characteristics;
    }

    @CheckForNull
    public Characteristic findCharacteristicByKey(final String key) {
      Characteristic characteristic = Iterables.find(characteristics, new Predicate<Characteristic>() {
        @Override
        public boolean apply(@Nullable Characteristic input) {
          return input != null && input.key.equals(key);
        }
      }, null);
      if (characteristic != null && characteristic.getParentId() != null) {
        throw MessageException.of(String.format("'%s' must be a characteristic. " + ERROR_SUFFIX, characteristic.getName()));
      }
      return characteristic;
    }

    @CheckForNull
    public Characteristic findSubCharacteristicByKey(final String key, Characteristic parent) {
      Characteristic characteristic = Iterables.find(characteristics, new Predicate<Characteristic>() {
        @Override
        public boolean apply(@Nullable Characteristic input) {
          return input != null && input.key.equals(key);
        }
      }, null);
      if (characteristic != null) {
        Integer parentId = characteristic.getParentId();
        if (parentId == null) {
          throw MessageException.of(String.format("'%s' must be a sub-characteristic. " + ERROR_SUFFIX, characteristic.getName()));
        } else if (!parentId.equals(parent.getId())) {
          throw MessageException.of(String.format("'%s' must be defined under '%s'. " + ERROR_SUFFIX, characteristic.getName(), parent.getName()));
        }
      }
      return characteristic;
    }

    private List<Characteristic> selectEnabledCharacteristics() throws SQLException {
      return context.prepareSelect(
        // Exclude requirements (to not fail when coming from a version older than 4.3)
        "SELECT c.id, c.kee, c.name, c.characteristic_order, c.parent_id FROM characteristics c WHERE c.enabled=? AND c.rule_id IS NULL ORDER BY c.characteristic_order")
        .setBoolean(1, true)
        .list(new CharacteristicReader());
    }

    private int selectCharacteristicId(String key) throws SQLException {
      Long id = context.prepareSelect(
        "SELECT c.id FROM characteristics c WHERE c.kee = ? AND c.enabled=?")
        .setString(1, key)
        .setBoolean(2, true)
        .get(Select.LONG_READER);
      if (id != null) {
        return id.intValue();
      } else {
        throw new IllegalStateException(String.format("Characteristic '%s' could not be inserted", key));
      }
    }

    public void insertCharacteristic(Characteristic characteristic) throws SQLException {
      if (characteristic.getParentId() == null) {
        LOGGER.info("Insert new characteristic '{}'", characteristic.getKey());
      } else {
        LOGGER.info("Insert new sub characteristic '{}'", characteristic.getKey());
      }

      context.prepareUpsert("INSERT INTO characteristics (kee, name, parent_id, characteristic_order, enabled, created_at) VALUES (?, ?, ?, ?, ?, ?)")
        .setString(1, characteristic.getKey())
        .setString(2, characteristic.getName())
        .setInt(3, characteristic.getParentId())
        .setInt(4, characteristic.getOrder())
        .setBoolean(5, true)
        .setDate(6, now)
        .execute()
        .commit();
      characteristic.setId(selectCharacteristicId(characteristic.getKey()));

      characteristics.add(characteristic);
    }

    public void updateCharacteristicOrder(String key, Integer order) throws SQLException {
      LOGGER.info("Update characteristic '{}' order to {}", key, order);

      context.prepareUpsert("UPDATE characteristics SET characteristic_order=?, updated_at=? WHERE kee=?")
        .setInt(1, order)
        .setDate(2, now)
        .setString(3, key)
        .execute()
        .commit();
    }

    private static class CharacteristicReader implements Select.RowReader<Characteristic> {
      @Override
      public Characteristic read(Select.Row row) throws SQLException {
        return new Characteristic()
          .setId(row.getInt(1))
          .setKey(row.getString(2))
          .setName(row.getString(3))
          .setOrder(row.getNullableInt(4))
          .setParentId(row.getNullableInt(5));
      }
    }
  }
}
