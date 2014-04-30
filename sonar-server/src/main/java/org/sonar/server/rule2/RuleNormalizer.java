package org.sonar.server.rule2;

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

  public enum RuleFields {
    KEY("key"),
    RULE_KEY("ruleKey"),
    REPOSITORY_KEY("repositoryKey"),
    NAME("name"),
    CREATED_AT("createdAt");

    private final String key;

    private RuleFields(final String key) {
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

    indexField(RuleFields.KEY.key(), rule.getKey(), document);
    indexField(RuleFields.NAME.key(), rule.getName(), document);
    indexField(RuleFields.CREATED_AT.key(), rule.getCreatedAt(), document);
    indexField(RuleFields.RULE_KEY.key(), rule.getRuleKey(), document);
    indexField(RuleFields.REPOSITORY_KEY.key(), rule.getRepositoryKey(), document);

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
