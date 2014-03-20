define(['handlebars'], function(Handlebars) {

this["SS"] = this["SS"] || {};
this["SS"]["Templates"] = this["SS"]["Templates"] || {};

Handlebars.registerPartial("_markdown-tips", Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;


  buffer += "<div class=\"markdown-tips\">\n  <a href=\"#\" onclick=\"window.open(baseUrl + '/markdown/help','markdown','height=300,width=600,scrollbars=1,resizable=1');return false;\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "markdown.helplink", options) : helperMissing.call(depth0, "t", "markdown.helplink", options)))
    + "</a> :\n  &nbsp; *"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "bold", options) : helperMissing.call(depth0, "t", "bold", options)))
    + "* &nbsp;&nbsp; ``"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "code", options) : helperMissing.call(depth0, "t", "code", options)))
    + "`` &nbsp;&nbsp; * "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "bulleted_point", options) : helperMissing.call(depth0, "t", "bulleted_point", options)))
    + "\n</div>";
  return buffer;
  }));

this["SS"]["Templates"]["coding-rules-actions"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, functionType="function", self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n    "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.ordered_by", options) : helperMissing.call(depth0, "t", "coding_rules.ordered_by", options)))
    + " <strong>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.sorting)),stack1 == null || stack1 === false ? stack1 : stack1.sortText)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</strong> ";
  stack1 = helpers['if'].call(depth0, ((stack1 = (depth0 && depth0.sorting)),stack1 == null || stack1 === false ? stack1 : stack1.asc), {hash:{},inverse:self.program(4, program4, data),fn:self.program(2, program2, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  ";
  return buffer;
  }
function program2(depth0,data) {
  
  
  return "<i class=\"icon-arrow-up\"></i>";
  }

function program4(depth0,data) {
  
  
  return "<i class=\"icon-arrow-down\"></i>";
  }

function program6(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n    "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.order", options) : helperMissing.call(depth0, "t", "coding_rules.order", options)))
    + "\n  ";
  return buffer;
  }

  buffer += "<div class=\"navigator-actions-order\">\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.sorting), {hash:{},inverse:self.program(6, program6, data),fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</div>\n<ul class=\"navigator-actions-order-choices\">\n  <li data-sort=\"CREATION_DATE\" data-asc=\"true\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.sort.creation_date", options) : helperMissing.call(depth0, "t", "coding_rules.sort.creation_date", options)))
    + " <i class=\"icon-arrow-up\"></i></li>\n  <li data-sort=\"CREATION_DATE\" data-asc=\"false\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.sort.creation_date", options) : helperMissing.call(depth0, "t", "coding_rules.sort.creation_date", options)))
    + " <i class=\"icon-arrow-down\"></i></li>\n  <li data-sort=\"NAME\" data-asc=\"true\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.sort.name", options) : helperMissing.call(depth0, "t", "coding_rules.sort.name", options)))
    + " <i class=\"icon-arrow-up\"></i></li>\n  <li data-sort=\"NAME\" data-asc=\"false\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.sort.name", options) : helperMissing.call(depth0, "t", "coding_rules.sort.name", options)))
    + " <i class=\"icon-arrow-down\"></i></li>\n</ul>\n<div class=\"navigator-actions-total\">\n  "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.found", options) : helperMissing.call(depth0, "t", "coding_rules.found", options)))
    + ": <strong>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.paging)),stack1 == null || stack1 === false ? stack1 : stack1.fTotal)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</strong>\n  <a class=\"navigator-actions-bulk\"\n     title=\""
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "bulk_change", options) : helperMissing.call(depth0, "t", "bulk_change", options)))
    + "\"><i class=\"icon-settings-multiple\"></i></a>\n</div>";
  return buffer;
  });

this["SS"]["Templates"]["coding-rules-bulk-change"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, functionType="function", escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing, self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n      <div class=\"modal-field\">\n        <label for=\"coding-rules-bulk-change-activate-on-qp\">Activate on</label>\n        <input id=\"coding-rules-bulk-change-activate-qp\" type=\"checkbox\">\n        <span class=\"text\">";
  if (helper = helpers.inactiveQualityProfileName) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.inactiveQualityProfileName); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</span>\n      </div>\n    ";
  return buffer;
  }

function program3(depth0,data) {
  
  
  return "Activate on";
  }

function program5(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n          <option value=\"";
  if (helper = helpers.key) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.key); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\">";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</option>\n        ";
  return buffer;
  }

function program7(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n      <div class=\"modal-field\">\n        <label for=\"coding-rules-bulk-change-deactivate-on-qp\">Deactivate on</label>\n        <input id=\"coding-rules-bulk-change-deactivate-qp\" type=\"checkbox\">\n        <span class=\"text\">";
  if (helper = helpers.activeQualityProfileName) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.activeQualityProfileName); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</span>\n      </div>\n    ";
  return buffer;
  }

function program9(depth0,data) {
  
  
  return "Deactivate on";
  }

function program11(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n      <div class=\"modal-field\">\n        <label for=\"coding-rules-bulk-change-severity\">Change Severity</label>\n        <input id=\"coding-rules-bulk-change-set-severity\" type=\"checkbox\">\n        <select id=\"coding-rules-bulk-change-severity\">\n          ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.severities), {hash:{},inverse:self.noop,fn:self.program(12, program12, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n        </select>\n      </div>\n    ";
  return buffer;
  }
function program12(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n            <option value=\""
    + escapeExpression((typeof depth0 === functionType ? depth0.apply(depth0) : depth0))
    + "\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "severity", depth0, options) : helperMissing.call(depth0, "t", "severity", depth0, options)))
    + "</option>\n          ";
  return buffer;
  }

  buffer += "<form>\n  <div class=\"modal-head\">\n    <h2>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.bulk_change", options) : helperMissing.call(depth0, "t", "coding_rules.bulk_change", options)))
    + " "
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.paging)),stack1 == null || stack1 === false ? stack1 : stack1.fTotal)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + " "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules._rules", options) : helperMissing.call(depth0, "t", "coding_rules._rules", options)))
    + "</h2>\n  </div>\n\n  <div class=\"modal-body\">\n    <div class=\"modal-error\"></div>\n\n    ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.inactiveQualityProfile), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n\n    <div class=\"modal-field\">\n      <label for=\"coding-rules-bulk-change-activate-on\">";
  stack1 = helpers.unless.call(depth0, (depth0 && depth0.inactiveQualityProfile), {hash:{},inverse:self.noop,fn:self.program(3, program3, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "</label>\n      <input id=\"coding-rules-bulk-change-activate\" type=\"checkbox\">\n      <select id=\"coding-rules-bulk-change-activate-on\" multiple>\n        ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.activateOnQualityProfiles), {hash:{},inverse:self.noop,fn:self.program(5, program5, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n      </select>\n    </div>\n\n    ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.activeQualityProfile), {hash:{},inverse:self.noop,fn:self.program(7, program7, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n\n    <div class=\"modal-field\">\n      <label for=\"coding-rules-bulk-change-deactivate-on\">";
  stack1 = helpers.unless.call(depth0, (depth0 && depth0.activeQualityProfile), {hash:{},inverse:self.noop,fn:self.program(9, program9, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "</label>\n      <input id=\"coding-rules-bulk-change-deactivate\" type=\"checkbox\">\n      <select id=\"coding-rules-bulk-change-deactivate-on\" multiple>\n        ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.deactivateOnQualityProfiles), {hash:{},inverse:self.noop,fn:self.program(5, program5, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n      </select>\n    </div>\n\n    ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.activeQualityProfile), {hash:{},inverse:self.noop,fn:self.program(11, program11, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  </div>\n\n  <div class=\"modal-foot\">\n    <button>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "apply", options) : helperMissing.call(depth0, "t", "apply", options)))
    + "</button>\n    <a id=\"coding-rules-cancel-bulk-change\" class=\"action\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "cancel", options) : helperMissing.call(depth0, "t", "cancel", options)))
    + "</a>\n  </div>\n</form>";
  return buffer;
  });

this["SS"]["Templates"]["coding-rules-detail-quality-profile"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); partials = this.merge(partials, Handlebars.partials); data = data || {};
  var buffer = "", stack1, helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, functionType="function", self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n  <div class=\"coding-rules-detail-quality-profile-inheritance\">\n    <i class=\"icon-inheritance\"></i> "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.inherits", options) : helperMissing.call(depth0, "t", "coding_rules.inherits", options)))
    + " <strong>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.parent)),stack1 == null || stack1 === false ? stack1 : stack1.name)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</strong>\n  </div>\n";
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n          <option value=\""
    + escapeExpression((typeof depth0 === functionType ? depth0.apply(depth0) : depth0))
    + "\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "severity", depth0, options) : helperMissing.call(depth0, "t", "severity", depth0, options)))
    + "</option>\n        ";
  return buffer;
  }

function program5(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n        ";
  stack1 = (helper = helpers.notEq || (depth0 && depth0.notEq),options={hash:{},inverse:self.noop,fn:self.program(6, program6, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.severity), ((stack1 = (depth0 && depth0.parent)),stack1 == null || stack1 === false ? stack1 : stack1.severity), options) : helperMissing.call(depth0, "notEq", (depth0 && depth0.severity), ((stack1 = (depth0 && depth0.parent)),stack1 == null || stack1 === false ? stack1 : stack1.severity), options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n      ";
  return buffer;
  }
function program6(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n          "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.original", options) : helperMissing.call(depth0, "t", "coding_rules.original", options)))
    + " "
    + escapeExpression((helper = helpers.severityIcon || (depth0 && depth0.severityIcon),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.parent)),stack1 == null || stack1 === false ? stack1 : stack1.severity), options) : helperMissing.call(depth0, "severityIcon", ((stack1 = (depth0 && depth0.parent)),stack1 == null || stack1 === false ? stack1 : stack1.severity), options)))
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "severity", ((stack1 = (depth0 && depth0.parent)),stack1 == null || stack1 === false ? stack1 : stack1.severity), options) : helperMissing.call(depth0, "t", "severity", ((stack1 = (depth0 && depth0.parent)),stack1 == null || stack1 === false ? stack1 : stack1.severity), options)))
    + "\n        ";
  return buffer;
  }

function program8(depth0,data,depth1) {
  
  var buffer = "", stack1, helper;
  buffer += "\n    <li class=\"coding-rules-detail-parameter\">\n      <h3 class=\"coding-rules-detail-parameter-name\">";
  if (helper = helpers.key) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.key); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</h3>\n      <div class=\"coding-rules-detail-parameter-description\">\n        <input type=\"text\" value=\"";
  if (helper = helpers.value) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.value); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\">\n        ";
  stack1 = helpers['if'].call(depth0, (depth1 && depth1.parent), {hash:{},inverse:self.noop,fn:self.program(9, program9, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n      </div>\n    </li>\n  ";
  return buffer;
  }
function program9(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n          ";
  stack1 = (helper = helpers.notEq || (depth0 && depth0.notEq),options={hash:{},inverse:self.noop,fn:self.program(10, program10, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.value), (depth0 && depth0.original), options) : helperMissing.call(depth0, "notEq", (depth0 && depth0.value), (depth0 && depth0.original), options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n        ";
  return buffer;
  }
function program10(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n            "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.original", options) : helperMissing.call(depth0, "t", "coding_rules.original", options)))
    + " ";
  if (helper = helpers.original) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.original); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\n          ";
  return buffer;
  }

function program12(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n    <blockquote class=\"rule-desc\">\n      <cite>\n        <b>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.note)),stack1 == null || stack1 === false ? stack1 : stack1.username)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</b> ("
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.note)),stack1 == null || stack1 === false ? stack1 : stack1.fCreationDate)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + ") &nbsp;|&nbsp;\n        <a class=\"coding-rules-detail-quality-profile-note-edit link-action\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "edit", options) : helperMissing.call(depth0, "t", "edit", options)))
    + "</a>&nbsp;\n        <a class=\"coding-rules-detail-quality-profile-note-delete link-action\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "delete", options) : helperMissing.call(depth0, "t", "delete", options)))
    + "</a>\n      </cite>\n      ";
  stack1 = ((stack1 = ((stack1 = (depth0 && depth0.note)),stack1 == null || stack1 === false ? stack1 : stack1.html)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1);
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    </blockquote>\n  ";
  return buffer;
  }

function program14(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n    <a class=\"coding-rules-detail-quality-profile-note-add link-action\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.add_note", options) : helperMissing.call(depth0, "t", "coding_rules.add_note", options)))
    + "</a>\n  ";
  return buffer;
  }

function program16(depth0,data) {
  
  var stack1;
  return escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.note)),stack1 == null || stack1 === false ? stack1 : stack1.raw)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1));
  }

function program18(depth0,data) {
  
  var helper, options;
  return escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "update", options) : helperMissing.call(depth0, "t", "update", options)));
  }

function program20(depth0,data) {
  
  var helper, options;
  return escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.add_note", options) : helperMissing.call(depth0, "t", "coding_rules.add_note", options)));
  }

function program22(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n    <button class=\"button-red\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.revert_to_parent_definition", options) : helperMissing.call(depth0, "t", "coding_rules.revert_to_parent_definition", options)))
    + "</button>\n  ";
  return buffer;
  }

  buffer += "<div class=\"coding-rules-detail-quality-profile-name\">";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</div>\n\n";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.parent), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n\n<ul class=\"coding-rules-detail-parameters coding-rules-detail-quality-profile-parameters\">\n  <li class=\"coding-rules-detail-parameter\">\n    <h3 class=\"coding-rules-detail-parameter-name\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "severity", options) : helperMissing.call(depth0, "t", "severity", options)))
    + "</h3>\n    <div class=\"coding-rules-detail-parameter-description\">\n      <select class=\"coding-rules-detail-quality-profile-severity\">\n        ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.severities), {hash:{},inverse:self.noop,fn:self.program(3, program3, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n      </select>\n      ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.parent), {hash:{},inverse:self.noop,fn:self.program(5, program5, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    </div>\n  </li>\n  ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.parameters), {hash:{},inverse:self.noop,fn:self.programWithDepth(8, program8, data, depth0),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</ul>\n\n<div class=\"coding-rules-detail-quality-profile-note\">\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.note), {hash:{},inverse:self.program(14, program14, data),fn:self.program(12, program12, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</div>\n\n<div class=\"coding-rules-detail-quality-profile-note-form admin\">\n  <table class=\"width100 table\">\n    <tbody>\n    <tr>\n      <td class=\"width100\" colspan=\"2\">\n        <textarea class=\"coding-rules-detail-quality-profile-note-text\" rows=\"4\" style=\"width: 100%;\">";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.note), {hash:{},inverse:self.noop,fn:self.program(16, program16, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "</textarea>\n      </td>\n    </tr>\n    <tr>\n      <td>\n        <button class=\"coding-rules-detail-quality-profile-note-submit\">\n          ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.note), {hash:{},inverse:self.program(20, program20, data),fn:self.program(18, program18, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n        </button>\n        <a class=\"coding-rules-detail-quality-profile-note-cancel action\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "cancel", options) : helperMissing.call(depth0, "t", "cancel", options)))
    + "</a>\n      </td>\n      <td class=\"right\">\n        ";
  stack1 = self.invokePartial(partials['_markdown-tips'], '_markdown-tips', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n      </td>\n    </tr>\n    </tbody>\n  </table>\n</div>\n\n<div class=\"button-group coding-rules-detail-quality-profile-actions\">\n  <button>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "update", options) : helperMissing.call(depth0, "t", "update", options)))
    + "</button>\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.parent), {hash:{},inverse:self.noop,fn:self.program(22, program22, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  <button class=\"button-red\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.deactivate_quality_profile", options) : helperMissing.call(depth0, "t", "coding_rules.deactivate_quality_profile", options)))
    + "</button>\n</div>";
  return buffer;
  });

this["SS"]["Templates"]["coding-rules-detail"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); partials = this.merge(partials, Handlebars.partials); data = data || {};
  var buffer = "", stack1, helper, options, functionType="function", escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing, self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n    <li class=\"coding-rules-detail-property\">\n      <span class=\"coding-rules-detail-status\">";
  if (helper = helpers.status) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.status); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</span>\n    </li>\n  ";
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "<div class=\"rule-desc\">";
  if (helper = helpers.extra) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.extra); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "</div>";
  return buffer;
  }

function program5(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n  <h3 class=\"coding-rules-detail-title\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.parameters", options) : helperMissing.call(depth0, "t", "coding_rules.parameters", options)))
    + "</h3>\n  <ul class=\"coding-rules-detail-parameters\">\n    ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.parameters), {hash:{},inverse:self.noop,fn:self.program(6, program6, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  </ul>\n";
  return buffer;
  }
function program6(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n      <li class=\"coding-rules-detail-parameter\">\n        <h3 class=\"coding-rules-detail-parameter-name\">";
  if (helper = helpers.key) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.key); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</h3>\n        <div class=\"coding-rules-detail-parameter-description\">\n          ";
  if (helper = helpers.description) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.description); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\n\n          ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0['default']), {hash:{},inverse:self.noop,fn:self.program(7, program7, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n        </div>\n      </li>\n    ";
  return buffer;
  }
function program7(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n            <div>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.parameters.default_value", options) : helperMissing.call(depth0, "t", "coding_rules.parameters.default_value", options)))
    + " ";
  if (helper = helpers['default']) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0['default']); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</div>\n          ";
  return buffer;
  }

  buffer += "<h3 class=\"coding-rules-detail-header\">";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</h3>\n\n<ul class=\"coding-rules-detail-properties\">\n  <li class=\"coding-rules-detail-property\">"
    + escapeExpression((helper = helpers.severityIcon || (depth0 && depth0.severityIcon),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.severity), options) : helperMissing.call(depth0, "severityIcon", (depth0 && depth0.severity), options)))
    + " "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "severity", (depth0 && depth0.severity), options) : helperMissing.call(depth0, "t", "severity", (depth0 && depth0.severity), options)))
    + "</li>\n  <li class=\"coding-rules-detail-property\">\n    <span class=\"coding-rules-detail-status\">";
  if (helper = helpers.language) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.language); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</span>\n  </li>\n  ";
  stack1 = (helper = helpers.notEq || (depth0 && depth0.notEq),options={hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.status), "READY", options) : helperMissing.call(depth0, "notEq", (depth0 && depth0.status), "READY", options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  <li class=\"coding-rules-detail-property coding-rules-detail-tag-list\">\n    <i class=\"icon-tags\"></i>\n    "
    + escapeExpression((helper = helpers.join || (depth0 && depth0.join),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.tags), ", ", options) : helperMissing.call(depth0, "join", (depth0 && depth0.tags), ", ", options)))
    + "\n    <a class=\"link-action coding-rules-detail-tags-change\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "change", options) : helperMissing.call(depth0, "t", "change", options)))
    + "</a>\n  </li>\n  <li class=\"coding-rules-detail-property coding-rules-detail-tag-edit\">\n    <input class=\"coding-rules-detail-tag-input\" type=\"text\" value=\""
    + escapeExpression((helper = helpers.join || (depth0 && depth0.join),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.tags), ",", options) : helperMissing.call(depth0, "join", (depth0 && depth0.tags), ",", options)))
    + "\">\n    <a class=\"link-action coding-rules-detail-tag-edit-done\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "done", options) : helperMissing.call(depth0, "t", "done", options)))
    + "</a>\n  </li>\n  <li class=\"coding-rules-detail-property\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.available_since", options) : helperMissing.call(depth0, "t", "coding_rules.available_since", options)))
    + " ";
  if (helper = helpers.fCreationDate) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.fCreationDate); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</li>\n  <li class=\"coding-rules-detail-property\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.repository", options) : helperMissing.call(depth0, "t", "coding_rules.repository", options)))
    + " ";
  if (helper = helpers.repository) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.repository); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</li>\n  <li class=\"coding-rules-detail-property\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.key", options) : helperMissing.call(depth0, "t", "coding_rules.key", options)))
    + " ";
  if (helper = helpers.key) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.key); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</li>\n</ul>\n\n<div class=\"coding-rules-detail-description rule-desc\">";
  if (helper = helpers.description) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.description); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "</div>\n\n<div class=\"coding-rules-detail-description coding-rules-detail-description-extra\">\n  <div id=\"coding-rules-detail-description-extra\">\n    ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.extra), {hash:{},inverse:self.noop,fn:self.program(3, program3, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    <a class=\"link-action\" id=\"coding-rules-detail-extend-description\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.extend_description", options) : helperMissing.call(depth0, "t", "coding_rules.extend_description", options)))
    + "</a>\n  </div>\n\n  <div id=\"coding-rules-detail-extend-description-form\" class=\"admin\">\n    <table class=\"width100 table\">\n      <tbody>\n      <tr>\n        <td class=\"width100\" colspan=\"2\">\n          <textarea id=\"coding-rules-detail-extend-description-text\" rows=\"4\" style=\"width: 100%;\">";
  if (helper = helpers.extraRaw) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.extraRaw); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</textarea>\n        </td>\n      </tr>\n      <tr>\n        <td>\n          <button id=\"coding-rules-detail-extend-description-submit\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.extend_description", options) : helperMissing.call(depth0, "t", "coding_rules.extend_description", options)))
    + "</button>\n          <a id=\"coding-rules-detail-extend-description-cancel\" class=\"action\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "cancel", options) : helperMissing.call(depth0, "t", "cancel", options)))
    + "</a>\n        </td>\n        <td class=\"right\">\n          ";
  stack1 = self.invokePartial(partials['_markdown-tips'], '_markdown-tips', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n        </td>\n      </tr>\n      </tbody>\n    </table>\n  </div>\n\n  <div id=\"coding-rules-detail-extend-description-spinner\">\n    <i class=\"spinner\"></i>\n  </div>\n</div>\n\n\n";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.parameters), {hash:{},inverse:self.noop,fn:self.program(5, program5, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n\n\n<h3 class=\"coding-rules-detail-title\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.quality_profiles", options) : helperMissing.call(depth0, "t", "coding_rules.quality_profiles", options)))
    + "</h3>\n<div class=\"button-group coding-rules-detail-quality-profiles-activation\">\n  <button id=\"coding-rules-quality-profile-activate\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.activate_quality_profile", options) : helperMissing.call(depth0, "t", "coding_rules.activate_quality_profile", options)))
    + "</button>\n</div>\n<div id=\"coding-rules-detail-quality-profiles\"></div>";
  return buffer;
  });

this["SS"]["Templates"]["coding-rules-facets-item"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, functionType="function", escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = "";
  buffer += "\n    <li class=\"navigator-facets-list-item-option\">\n      <span class=\"navigator-facets-list-item-option-name\">"
    + escapeExpression((typeof depth0 === functionType ? depth0.apply(depth0) : depth0))
    + "</span>\n      <span class=\"navigator-facets-list-item-option-stat subtitle\">42</span>\n    </li>\n  ";
  return buffer;
  }

  buffer += "<h3 class=\"navigator-facets-list-item-name\">";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</h3>\n<ul class=\"navigator-facets-list-item-options\">\n  ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.options), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</ul>";
  return buffer;
  });

this["SS"]["Templates"]["coding-rules-filter-bar"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;


  buffer += "<div class=\"navigator-filters-list\"></div>\n<button class=\"navigator-filter-submit\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "search_verb", options) : helperMissing.call(depth0, "t", "search_verb", options)))
    + "</button>";
  return buffer;
  });

this["SS"]["Templates"]["coding-rules-header"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;


  buffer += "<h1 class=\"navigator-header-title\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.page", options) : helperMissing.call(depth0, "t", "coding_rules.page", options)))
    + "</h1>\n\n<div class=\"navigator-header-actions button-group\">\n  <button id=\"coding-rules-new-search\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.new_search", options) : helperMissing.call(depth0, "t", "coding_rules.new_search", options)))
    + "</button>\n</div>";
  return buffer;
  });

this["SS"]["Templates"]["coding-rules-layout"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  


  return "<div class=\"navigator-header\"></div>\n<div class=\"navigator-filters\"></div>\n<div class=\"navigator-results\"></div>\n<div class=\"navigator-actions\"></div>\n<div class=\"navigator-details\"></div>";
  });

this["SS"]["Templates"]["coding-rules-list-empty"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;


  return escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.no_results", options) : helperMissing.call(depth0, "t", "coding_rules.no_results", options)));
  });

this["SS"]["Templates"]["coding-rules-list-item"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, functionType="function", escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing, self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n    <i class=\"icon-quality-profile\"></i>&nbsp;";
  if (helper = helpers.qualityProfileName) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.qualityProfileName); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "&nbsp;\n    "
    + escapeExpression((helper = helpers.severityIcon || (depth0 && depth0.severityIcon),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.severity), options) : helperMissing.call(depth0, "severityIcon", (depth0 && depth0.severity), options)))
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "severity", (depth0 && depth0.severity), options) : helperMissing.call(depth0, "t", "severity", (depth0 && depth0.severity), options)))
    + "&nbsp;\n  ";
  return buffer;
  }

function program3(depth0,data) {
  
  var stack1, helper;
  if (helper = helpers.status) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.status); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  return escapeExpression(stack1);
  }

  buffer += "<div class=\"line line-small\">\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.qualityProfile), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  <span class=\"coding-rules-detail-status\">";
  if (helper = helpers.language) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.language); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</span>\n\n  <div class=\"line-right\">";
  stack1 = (helper = helpers.notEq || (depth0 && depth0.notEq),options={hash:{},inverse:self.noop,fn:self.program(3, program3, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.status), "READY", options) : helperMissing.call(depth0, "notEq", (depth0 && depth0.status), "READY", options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "</div>\n</div>\n<div class=\"line line-nowrap\" title=\"";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\">";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</div>";
  return buffer;
  });

this["SS"]["Templates"]["coding-rules-quality-profile-activation"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, functionType="function", escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing, self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n              <option value=\"";
  if (helper = helpers.key) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.key); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\">";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</option>\n            ";
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n              <option value=\""
    + escapeExpression((typeof depth0 === functionType ? depth0.apply(depth0) : depth0))
    + "\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "severity", depth0, options) : helperMissing.call(depth0, "t", "severity", depth0, options)))
    + "</option>\n            ";
  return buffer;
  }

function program5(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n        <tr class=\"property\">\n          <th><h3>";
  if (helper = helpers.key) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.key); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</h3></th>\n          <td>\n            <input type=\"text\" ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0['default']), {hash:{},inverse:self.noop,fn:self.program(6, program6, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += ">\n            <div class=\"note\">";
  if (helper = helpers.description) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.description); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</div>\n            ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.extra), {hash:{},inverse:self.noop,fn:self.program(8, program8, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n          </td>\n        </tr>\n      ";
  return buffer;
  }
function program6(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "value=\"";
  if (helper = helpers['default']) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0['default']); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\"";
  return buffer;
  }

function program8(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n              <div class=\"note\">";
  if (helper = helpers.extra) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.extra); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</div>\n            ";
  return buffer;
  }

  buffer += "<form>\n  <div class=\"modal-head\">\n    <h2>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.activate_quality_profile", options) : helperMissing.call(depth0, "t", "coding_rules.activate_quality_profile", options)))
    + "</h2>\n  </div>\n\n  <div class=\"modal-body\">\n    <div class=\"modal-error\"></div>\n\n    <table>\n      <tr class=\"property\">\n        <th><h3>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.quality_profile", options) : helperMissing.call(depth0, "t", "coding_rules.quality_profile", options)))
    + "</h3></th>\n        <td>\n          <select id=\"coding-rules-quality-profile-activation-select\">\n            ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.qualityProfiles), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n          </select>\n        </td>\n      </tr>\n      <tr class=\"property\">\n        <th><h3>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "severity", options) : helperMissing.call(depth0, "t", "severity", options)))
    + "</h3></th>\n        <td>\n          <select id=\"coding-rules-quality-profile-activation-severity\">\n            ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.severities), {hash:{},inverse:self.noop,fn:self.program(3, program3, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n          </select>\n        </td>\n      </tr>\n      ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.parameters), {hash:{},inverse:self.noop,fn:self.program(5, program5, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    </table>\n  </div>\n\n  <div class=\"modal-foot\">\n    <button id=\"coding-rules-quality-profile-activation-activate\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "coding_rules.activate", options) : helperMissing.call(depth0, "t", "coding_rules.activate", options)))
    + "</button>\n    <a id=\"coding-rules-quality-profile-activation-cancel\" class=\"action\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "cancel", options) : helperMissing.call(depth0, "t", "cancel", options)))
    + "</a>\n  </div>\n</form>";
  return buffer;
  });

return this["SS"]["Templates"];

});