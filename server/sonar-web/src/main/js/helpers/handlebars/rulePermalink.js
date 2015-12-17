module.exports = function (ruleKey) {
  return baseUrl + '/coding_rules#rule_key=' + encodeURIComponent(ruleKey);
};
