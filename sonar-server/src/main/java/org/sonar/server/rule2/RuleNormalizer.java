package org.sonar.server.rule2;

import org.sonar.check.Cardinality;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.search.BaseNormalizer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RuleNormalizer extends BaseNormalizer<RuleDto, RuleKey> {

  private RuleDao ruleDao;
  private ActiveRuleDao activeRuleDao;

  public enum RuleField {
    KEY("key"),
    REPOSITORY("repo"),
    NAME("name"),
    CREATED_AT("createdAt"),
    DESCRIPTION("desc"),
    SEVERITY("severity"),
    STATUS("status"),
    LANGUAGE("lang"),
    TAGS("tags"),
    SYSTEM_TAGS("sysTags"),
    INTERNAL_KEY("internalKey"),
    TEMPLATE("template"),
    UDPATED_AT("updatedAt");

    private final String key;

    private RuleField(final String key) {
      this.key = key;
    }

    public String key() {
      return key;
    }

    public String toString() {
      return key;
    }
  }

  public RuleNormalizer(RuleDao ruleDao, ActiveRuleDao activeRuleDao) {
    this.ruleDao = ruleDao;
    this.activeRuleDao = activeRuleDao;
  }

  @Override
  public XContentBuilder normalize(RuleKey key) throws IOException {
    return normalize(ruleDao.getByKey(key));
  }

  @Override
  public XContentBuilder normalize(RuleDto rule) throws IOException {

    XContentBuilder document = jsonBuilder().startObject();


    indexField(RuleField.KEY.key(), rule.getRuleKey(), document);
    indexField(RuleField.REPOSITORY.key(), rule.getRepositoryKey(), document);
    indexField(RuleField.NAME.key(), rule.getName(), document);
    indexField(RuleField.CREATED_AT.key(), rule.getCreatedAt(), document);
    indexField(RuleField.UDPATED_AT.key(), rule.getUpdatedAt(), document);
    indexField(RuleField.DESCRIPTION.key(), rule.getDescription(), document);
    indexField(RuleField.SEVERITY.key(), rule.getSeverityString(), document);
    indexField(RuleField.STATUS.key(), rule.getStatus(), document);
    indexField(RuleField.LANGUAGE.key(), rule.getLanguage(), document);
    indexField(RuleField.INTERNAL_KEY.key(), rule.getConfigKey(), document);
    indexField(RuleField.TEMPLATE.key(), rule.getCardinality()==Cardinality.MULTIPLE, document);

    indexField(RuleField.TAGS.key(), rule.getName(), document);
    indexField(RuleField.SYSTEM_TAGS.key(), rule.getName(), document);

    // document.startArray("active");
    // for (ActiveRuleDto activeRule : activeRuleDao.selectByRuleId(rule.getId())) {
    // document.startObject();
    // Map<String, Object> activeRuleProperties = BeanUtils.describe(activeRule);
    // for (Entry<String, Object> activeRuleProp : activeRuleProperties.entrySet()) {
    // LOG.trace("NORMALIZING: --- {} -> {}", activeRuleProp.getKey(), activeRuleProp.getValue());
    // document.field(activeRuleProp.getKey(), activeRuleProp.getValue());
    // }
    // document.endObject();
    // }
    // document.endArray();

    return document.endObject();
  }

}
