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
package org.sonar.server.qualityprofile.index;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.sonar.api.rules.ActiveRule;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.server.es.ESNode;
import org.sonar.server.rule2.index.RuleIndexDefinition;
import org.sonar.server.search.BaseIndex;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ActiveRuleIndex extends BaseIndex<ActiveRule, ActiveRuleDto, ActiveRuleKey> {

  public ActiveRuleIndex(ActiveRuleNormalizer normalizer, WorkQueue workQueue, ESNode node) {
    super(new ActiveRuleIndexDefinition(), normalizer, workQueue, node);
  }

  @Override
  protected String getKeyValue(ActiveRuleKey key) {
    return key.toString();
  }

  @Override
  protected XContentBuilder getIndexSettings() throws IOException {
    return null;
  }

  @Override
  protected XContentBuilder getMapping() throws IOException {
    XContentBuilder mapping = jsonBuilder().startObject()
      .startObject(this.indexDefinition.getIndexType())
      .field("dynamic", "strict")
      .startObject("_parent")
      .field("type", new RuleIndexDefinition().getIndexType())
      .endObject()
      .startObject("_id")
      .field("path", ActiveRuleNormalizer.ActiveRuleField.KEY.key())
      .endObject()
      .startObject("_routing")
      .field("required", true)
      .field("path", ActiveRuleNormalizer.ActiveRuleField.RULE_KEY.key())
      .endObject();


    mapping.startObject("properties");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.KEY.key(), "string");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.RULE_KEY.key(), "string");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.key(), "string");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.SEVERITY.key(), "string");
    addMatchField(mapping, ActiveRuleNormalizer.ActiveRuleField.PROFILE_ID.key(), "string");
    mapping.startObject(ActiveRuleNormalizer.ActiveRuleField.PARAMS.key())
      .field("type", "nested")
      .endObject();
    mapping.endObject();

    mapping.endObject().endObject();
    return mapping;
  }

  @Override
  public ActiveRule toDoc(GetResponse key) {
    return null;
  }
}
