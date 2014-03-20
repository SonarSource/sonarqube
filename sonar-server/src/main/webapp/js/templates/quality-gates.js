define(['handlebars'], function(Handlebars) {

this["SS"] = this["SS"] || {};
this["SS"]["Templates"] = this["SS"]["Templates"] || {};

this["SS"]["Templates"]["quality-gate-actions"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n  <div class=\"navigator-header-actions button-group\">\n    <button id=\"quality-gate-add\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "add_verb", options) : helperMissing.call(depth0, "t", "add_verb", options)))
    + "</button>\n  </div>\n";
  return buffer;
  }

  buffer += "<h1 class=\"navigator-header-title\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.page", options) : helperMissing.call(depth0, "t", "quality_gates.page", options)))
    + "</h1>\n";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.canEdit), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  return buffer;
  });

this["SS"]["Templates"]["quality-gate-detail-condition"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, functionType="function", self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n    <select name=\"period\">\n      ";
  stack1 = helpers.unless.call(depth0, (depth0 && depth0.isDiffMetric), {hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n      ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.periods), {hash:{},inverse:self.noop,fn:self.program(4, program4, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    </select>\n  ";
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "<option value=\"0\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "value", options) : helperMissing.call(depth0, "t", "value", options)))
    + "</option>";
  return buffer;
  }

function program4(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "<option value=\"";
  if (helper = helpers.key) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.key); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\">&Delta; ";
  if (helper = helpers.text) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.text); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</option>";
  return buffer;
  }

function program6(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n    ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.periodText), {hash:{},inverse:self.program(9, program9, data),fn:self.program(7, program7, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  ";
  return buffer;
  }
function program7(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "&Delta; ";
  if (helper = helpers.periodText) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.periodText); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\n    ";
  return buffer;
  }

function program9(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "value", options) : helperMissing.call(depth0, "t", "value", options)))
    + "\n    ";
  return buffer;
  }

function program11(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n    <select name=\"operator\">\n      ";
  stack1 = (helper = helpers.operators || (depth0 && depth0.operators),options={hash:{},inverse:self.noop,fn:self.program(12, program12, data),data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.metric)),stack1 == null || stack1 === false ? stack1 : stack1.type), options) : helperMissing.call(depth0, "operators", ((stack1 = (depth0 && depth0.metric)),stack1 == null || stack1 === false ? stack1 : stack1.type), options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    </select>\n  ";
  return buffer;
  }
function program12(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n        <option value=\""
    + escapeExpression((typeof depth0 === functionType ? depth0.apply(depth0) : depth0))
    + "\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.operator", depth0, options) : helperMissing.call(depth0, "t", "quality_gates.operator", depth0, options)))
    + "</option>\n      ";
  return buffer;
  }

function program14(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n    "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.operator", (depth0 && depth0.op), options) : helperMissing.call(depth0, "t", "quality_gates.operator", (depth0 && depth0.op), options)))
    + "\n  ";
  return buffer;
  }

function program16(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n    <input name=\"warning\" class=\"measure-input\" data-type=\""
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.metric)),stack1 == null || stack1 === false ? stack1 : stack1.type)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "\" type=\"text\">\n  ";
  return buffer;
  }

function program18(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n    ";
  if (helper = helpers.warning) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.warning); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\n  ";
  return buffer;
  }

function program20(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n    <input name=\"error\" class=\"measure-input\" data-type=\""
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.metric)),stack1 == null || stack1 === false ? stack1 : stack1.type)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "\" type=\"text\">\n  ";
  return buffer;
  }

function program22(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n    ";
  if (helper = helpers.error) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.error); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\n  ";
  return buffer;
  }

function program24(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n    ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.id), {hash:{},inverse:self.program(27, program27, data),fn:self.program(25, program25, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  ";
  return buffer;
  }
function program25(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n      <div class=\"button-group\">\n        <button class=\"update-condition\" disabled>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "update_verb", options) : helperMissing.call(depth0, "t", "update_verb", options)))
    + "</button>\n        <button class=\"button-red delete-condition\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "delete", options) : helperMissing.call(depth0, "t", "delete", options)))
    + "</button>\n      </div>\n    ";
  return buffer;
  }

function program27(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n      <div class=\"button-group\">\n        <button class=\"add-condition\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "add_verb", options) : helperMissing.call(depth0, "t", "add_verb", options)))
    + "</button>\n        <a class=\"action cancel-add-condition\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "cancel", options) : helperMissing.call(depth0, "t", "cancel", options)))
    + "</a>\n      </div>\n    ";
  return buffer;
  }

  buffer += "<td nowrap>\n  "
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.metric)),stack1 == null || stack1 === false ? stack1 : stack1.name)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "\n</td>\n<td width=\"10%\" nowrap>\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.canEdit), {hash:{},inverse:self.program(6, program6, data),fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</td>\n<td width=\"10%\" nowrap>\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.canEdit), {hash:{},inverse:self.program(14, program14, data),fn:self.program(11, program11, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</td>\n<td width=\"15%\" nowrap=\"nowrap\">\n  <i class=\"icon-alert-warn\" title=\""
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "alerts.warning_tooltip", options) : helperMissing.call(depth0, "t", "alerts.warning_tooltip", options)))
    + "\"></i>\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.canEdit), {hash:{},inverse:self.program(18, program18, data),fn:self.program(16, program16, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</td>\n<td width=\"15%\" nowrap=\"nowrap\">\n  <i class=\"icon-alert-error\" title=\""
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "alerts.error_tooltip", options) : helperMissing.call(depth0, "t", "alerts.error_tooltip", options)))
    + "\"></i>\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.canEdit), {hash:{},inverse:self.program(22, program22, data),fn:self.program(20, program20, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</td>\n<td class=\"quality-gate-condition-actions\" width=\"120px\" nowrap>\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.canEdit), {hash:{},inverse:self.noop,fn:self.program(24, program24, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</td>";
  return buffer;
  });

this["SS"]["Templates"]["quality-gate-detail-conditions-empty"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;


  buffer += "<td colspan=\"6\">\n  "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.no_conditions", options) : helperMissing.call(depth0, "t", "quality_gates.no_conditions", options)))
    + "\n</td>";
  return buffer;
  });

this["SS"]["Templates"]["quality-gate-detail-conditions"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, functionType="function", escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n  <div class=\"quality-gate-new-condition\">\n    <label for=\"quality-gate-new-condition-metric\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.add_condition", options) : helperMissing.call(depth0, "t", "quality_gates.add_condition", options)))
    + ":</label>\n    <select id=\"quality-gate-new-condition-metric\">\n      <option></option>\n      ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.metricGroups), {hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    </select>\n  </div>\n";
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n        <optgroup label=\"";
  if (helper = helpers.domain) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.domain); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\">\n            ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.metrics), {hash:{},inverse:self.noop,fn:self.program(3, program3, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n        </optgroup>\n      ";
  return buffer;
  }
function program3(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "<option value=\"";
  if (helper = helpers.key) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.key); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\">";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</option>";
  return buffer;
  }

  buffer += "<div class=\"quality-gate-section-name\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.conditions", options) : helperMissing.call(depth0, "t", "quality_gates.conditions", options)))
    + "</div>\n\n<div class=\"quality-gate-introduction\">\n  <p>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.introduction", options) : helperMissing.call(depth0, "t", "quality_gates.introduction", options)))
    + "\n    <a class=\"link-action quality-gate-introduction-show-more\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "more", options) : helperMissing.call(depth0, "t", "more", options)))
    + "</a>\n  </p>\n  <div class=\"quality-gate-introduction-more inline-help\">\n    "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.health_icons", options) : helperMissing.call(depth0, "t", "quality_gates.health_icons", options)))
    + "\n    <ul>\n      <li>\n        <i class=\"icon-alert-ok\"></i>\n        "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "alerts.notes.ok", options) : helperMissing.call(depth0, "t", "alerts.notes.ok", options)))
    + "\n      </li>\n      <li>\n        <i class=\"icon-alert-warn\"></i>\n        "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "alerts.notes.warn", options) : helperMissing.call(depth0, "t", "alerts.notes.warn", options)))
    + "\n      </li>\n      <li>\n        <i class=\"icon-alert-error\"></i>\n        "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "alerts.notes.error", options) : helperMissing.call(depth0, "t", "alerts.notes.error", options)))
    + "\n      </li>\n    </ul>\n  </div>\n</div>\n\n";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.canEdit), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n\n<table class=\"data zebra width100 marginbottom10 spaced quality-gate-conditions\">\n  <thead><tr></tr></thead>\n  <tbody></tbody>\n</table>";
  return buffer;
  });

this["SS"]["Templates"]["quality-gate-detail-header"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this, functionType="function";

function program1(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n  <div class=\"navigator-header-actions button-group\">\n    <button id=\"quality-gate-rename\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "rename", options) : helperMissing.call(depth0, "t", "rename", options)))
    + "</button>\n    <button id=\"quality-gate-copy\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "copy", options) : helperMissing.call(depth0, "t", "copy", options)))
    + "</button>\n    ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0['default']), {hash:{},inverse:self.program(4, program4, data),fn:self.program(2, program2, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    <button id=\"quality-gate-delete\" class=\"button-red\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "delete", options) : helperMissing.call(depth0, "t", "delete", options)))
    + "</button>\n  </div>\n";
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n      <button id=\"quality-gate-unset-as-default\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "unset_as_default", options) : helperMissing.call(depth0, "t", "unset_as_default", options)))
    + "</button>\n    ";
  return buffer;
  }

function program4(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n      <button id=\"quality-gate-set-as-default\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "set_as_default", options) : helperMissing.call(depth0, "t", "set_as_default", options)))
    + "</button>\n    ";
  return buffer;
  }

  buffer += "<h1 class=\"navigator-header-title\">";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</h1>\n\n";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.canEdit), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  return buffer;
  });

this["SS"]["Templates"]["quality-gate-detail-projects"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n  <p class=\"quality-gate-default-message\">\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.canEdit), {hash:{},inverse:self.program(4, program4, data),fn:self.program(2, program2, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  </p>\n";
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n    "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.projects_for_default.edit", options) : helperMissing.call(depth0, "t", "quality_gates.projects_for_default.edit", options)))
    + "\n  ";
  return buffer;
  }

function program4(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n    "
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.projects_for_default", options) : helperMissing.call(depth0, "t", "quality_gates.projects_for_default", options)))
    + "\n  ";
  return buffer;
  }

function program6(depth0,data) {
  
  
  return "\n  <div id=\"select-list-projects\"></div>\n";
  }

  buffer += "<div class=\"quality-gate-section-name\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.projects", options) : helperMissing.call(depth0, "t", "quality_gates.projects", options)))
    + "</div>\n\n";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0['default']), {hash:{},inverse:self.program(6, program6, data),fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  return buffer;
  });

this["SS"]["Templates"]["quality-gate-detail"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  


  return "<div id=\"quality-gate-conditions\" class=\"quality-gate-section\"></div>\n<div id=\"quality-gate-projects\" class=\"quality-gate-section\"></div>";
  });

this["SS"]["Templates"]["quality-gate-edit"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, functionType="function", self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "<h2>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.rename", options) : helperMissing.call(depth0, "t", "quality_gates.rename", options)))
    + " ";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</h2>";
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "<h2>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.copy", options) : helperMissing.call(depth0, "t", "quality_gates.copy", options)))
    + " ";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</h2>";
  return buffer;
  }

function program5(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "<h2>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.add", options) : helperMissing.call(depth0, "t", "quality_gates.add", options)))
    + "</h2>";
  return buffer;
  }

function program7(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "<button>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "save", options) : helperMissing.call(depth0, "t", "save", options)))
    + "</button>";
  return buffer;
  }

function program9(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "<button>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "copy", options) : helperMissing.call(depth0, "t", "copy", options)))
    + "</button>";
  return buffer;
  }

function program11(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "<button>"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "create", options) : helperMissing.call(depth0, "t", "create", options)))
    + "</button>";
  return buffer;
  }

  buffer += "<form>\n  <div class=\"modal-head\">\n    ";
  stack1 = (helper = helpers.eq || (depth0 && depth0.eq),options={hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.method), "rename", options) : helperMissing.call(depth0, "eq", (depth0 && depth0.method), "rename", options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    ";
  stack1 = (helper = helpers.eq || (depth0 && depth0.eq),options={hash:{},inverse:self.noop,fn:self.program(3, program3, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.method), "copy", options) : helperMissing.call(depth0, "eq", (depth0 && depth0.method), "copy", options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    ";
  stack1 = (helper = helpers.eq || (depth0 && depth0.eq),options={hash:{},inverse:self.noop,fn:self.program(5, program5, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.method), "create", options) : helperMissing.call(depth0, "eq", (depth0 && depth0.method), "create", options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  </div>\n\n  <div class=\"modal-body\">\n    <div class=\"modal-error\"></div>\n    <div class=\"modal-field\">\n      <label for=\"quality-gate-edit-name\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "name", options) : helperMissing.call(depth0, "t", "name", options)))
    + " <em class=\"mandatory\">*</em></label>\n      <input id=\"quality-gate-edit-name\" type=\"text\" size=\"50\" maxlength=\"100\">\n    </div>\n  </div>\n\n  <div class=\"modal-foot\">\n    ";
  stack1 = (helper = helpers.eq || (depth0 && depth0.eq),options={hash:{},inverse:self.noop,fn:self.program(7, program7, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.method), "rename", options) : helperMissing.call(depth0, "eq", (depth0 && depth0.method), "rename", options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    ";
  stack1 = (helper = helpers.eq || (depth0 && depth0.eq),options={hash:{},inverse:self.noop,fn:self.program(9, program9, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.method), "copy", options) : helperMissing.call(depth0, "eq", (depth0 && depth0.method), "copy", options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    ";
  stack1 = (helper = helpers.eq || (depth0 && depth0.eq),options={hash:{},inverse:self.noop,fn:self.program(11, program11, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.method), "create", options) : helperMissing.call(depth0, "eq", (depth0 && depth0.method), "create", options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    <a id=\"quality-gate-cancel-create\" class=\"action\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "cancel", options) : helperMissing.call(depth0, "t", "cancel", options)))
    + "</a>\n  </div>\n</form>";
  return buffer;
  });

this["SS"]["Templates"]["quality-gate-sidebar-list-empty"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;


  buffer += "<div class=\"line line-nowrap\">"
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "quality_gates.noQualityGates", options) : helperMissing.call(depth0, "t", "quality_gates.noQualityGates", options)))
    + "</div>";
  return buffer;
  });

this["SS"]["Templates"]["quality-gate-sidebar-list-item"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, functionType="function", self=this;

function program1(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "<span class=\"subtitle\">("
    + escapeExpression((helper = helpers.t || (depth0 && depth0.t),options={hash:{},data:data},helper ? helper.call(depth0, "default", options) : helperMissing.call(depth0, "t", "default", options)))
    + ")</span>";
  return buffer;
  }

  buffer += "<div class=\"line line-nowrap\">";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + " ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0['default']), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "</div>";
  return buffer;
  });

this["SS"]["Templates"]["quality-gates-layout"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  


  return "<div class=\"navigator-header\"></div>\n<div class=\"navigator-results quality-gates-nav\"></div>\n<div class=\"navigator-details\"></div>\n<div class=\"navigator-actions\"></div>";
  });

return this["SS"]["Templates"];

});