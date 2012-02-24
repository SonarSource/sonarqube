/*
 Functions used in resource viewers
 */

function loadResourceViewer(resourceId, tab, display_title) {
  $('resource_loading').show();
  if (display_title == undefined) {
    display_title = true;
  }
  new Ajax.Updater('resource_container', baseUrl + '/resource/index/' + resourceId + '?tab=' + tab + '&display_title=' + display_title, {asynchronous:true, evalScripts:true});
  return false;
}

// Display GWT component
function loadGWT(gwtId, resourceId, resourceKey, resourceName, resourceScope, resourceQualifier, resourceLanguage) {
  config["resource"] = [
    {"id":resourceId, "key":resourceKey, "name":resourceName, "scope":resourceScope, "qualifier":resourceQualifier, "lang":resourceLanguage}
  ];
  config["resource_key"] = resourceId;
  modules[gwtId]();
}

// cancel action : hide form and refresh violation
function cancelViolationAction(violation_id) {
  new Ajax.Updater(
      'vId' + violation_id,
      baseUrl + '/reviews/display_violation/' + violation_id,
      {
        asynchronous:true,
        evalScripts:true
      });
  return false;
}

function hideMoreViolationActions(violation_id) {
  var popup = $('more' + violation_id);
  if (popup != null) {
    popup.hide();
  }
}

// show the form to comment violation
function sCF(violation_id) {
  hideMoreViolationActions(violation_id);
  new Ajax.Updater('reviewForm' + violation_id,
      baseUrl + '/reviews/violation_comment_form/' + violation_id,
      {
        asynchronous:true,
        evalScripts:true,
        onComplete:function (request) {
          $('vActions' + violation_id).remove();
          $('reviewForm' + violation_id).show();
          $('commentText' + violation_id).focus();
        }
      });
  return false;
}

// show the form to change severity
function sCSF(violation_id) {
  hideMoreViolationActions(violation_id);
  new Ajax.Updater('reviewForm' + violation_id,
      baseUrl + '/reviews/violation_change_severity_form/' + violation_id,
      {
        asynchronous:true,
        evalScripts:true,
        onComplete:function (request) {
          $('vActions' + violation_id).remove();
          $('reviewForm' + violation_id).show();
          $('selectSeverity' + violation_id).focus();
        }
      });
  return false;
}

// show the form to change status
function sCStF(violation_id) {
  hideMoreViolationActions(violation_id);
  new Ajax.Updater('reviewForm' + violation_id,
      baseUrl + '/reviews/violation_change_status_form/' + violation_id,
      {
        asynchronous:true,
        evalScripts:true,
        onComplete:function (request) {
          $('vActions' + violation_id).remove();
          $('reviewForm' + violation_id).show();
          $('commentText' + violation_id).focus();
        }
      });
  return false;
}

// show the form to flag as false-positive
function sFPF(violation_id) {
  hideMoreViolationActions(violation_id);
  new Ajax.Updater('reviewForm' + violation_id,
      baseUrl + '/reviews/violation_false_positive_form/' + violation_id,
      {
        asynchronous:true,
        evalScripts:true,
        onComplete:function (request) {
          $('vActions' + violation_id).remove();
          $('reviewForm' + violation_id).show();
          $('commentText' + violation_id).focus();
        }
      });
  return false;
}

// show the form to assign violation
function sAF(violation_id) {
  hideMoreViolationActions(violation_id);
  new Ajax.Updater('reviewForm' + violation_id,
      baseUrl + '/reviews/violation_assign_form/' + violation_id,
      {
        asynchronous:true,
        evalScripts:true,
        onComplete:function (request) {
          $('vActions' + violation_id).remove();
          $('reviewForm' + violation_id).show();
          $('assignee_login').focus();
        }
      });
  return false;
}

// show the form to link a review to an action plan
function sAPF(violation_id) {
  hideMoreViolationActions(violation_id);
  new Ajax.Updater('reviewForm' + violation_id,
      baseUrl + '/reviews/violation_action_plan_form/' + violation_id,
      {
        asynchronous:true,
        evalScripts:true,
        onComplete:function (request) {
          $('vActions' + violation_id).remove();
          $('reviewForm' + violation_id).show();
          $('action_plan').focus();
        }
      });
  return false;
}

// show the form to create violation
function sVF(resource, line, gray_colspan, white_colspan) {
  row = $('createViolationForm' + line);
  if (row == null) {
    new Ajax.Updater(
        'pos' + line,
        baseUrl + '/resource/show_create_violation_form',
        {
          parameters:{resource:resource, line:line, gray_colspan:gray_colspan, white_colspan:white_colspan},
          asynchronous:true,
          evalScripts:true,
          insertion:'after'
        });
  }
  return false;
}

// hide review form
function hVF(line) {
  row = $('createViolationRow' + line);
  if (row != null) {
    row.remove();
  }
  return false;
}