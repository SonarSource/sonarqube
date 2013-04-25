/*
 Functions used in resource viewers
 */

function loadResourceViewer(resourceId, tab, display_title, elt) {
  if (display_title == undefined) {
    display_title = true;
  }

  var url = baseUrl + '/resource/index/' + resourceId + '?tab=' + tab + '&display_title=' + display_title;
  openAccordionItem(url, elt, true);

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

// show review screen
function sS(violation_id, command_key) {
  hideMoreViolationActions(violation_id);
  new Ajax.Updater('reviewForm' + violation_id,
      baseUrl + '/reviews/screen/' + violation_id + '?command=' + command_key,
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
function sVF(elt, resource, line, gray_colspan, white_colspan) {
  row = $j('#createViolationForm' + line);
  if (!row.length) {
    expandAccordionItem(elt);
    var element = $j(elt).closest('.pos' + line);
    $j.get(baseUrl + '/resource/show_create_violation_form?resource='+ resource + '&line='+ line + '&gray_colspan='+ gray_colspan + '&white_colspan='+ white_colspan, function (html) {
      element.after(html);
    }).error(function () {
          alert("Server error. Please contact your administrator.");
        });
  }
  return false;
}

// hide review form
function hVF(elt, line) {
  var row = $j(elt).closest('#createViolationRow'+ line);
  if (row.length) {
    row.remove();
  }
  return false;
}

/*
 Functions used in tests viewer
 */
function expandTests(index, elt){
  expandAccordionItem(elt);
  var parent = $j(elt).closest('.test_name_'+index);
  parent.find(".test_expandLink_"+ index).hide();
  parent.find(".test_collapseLink_"+ index).show();
  parent.next(".tests_viewer .test_message_"+ index).show();
}

function collapseTests(index, elt){
  expandAccordionItem(elt);
  var parent = $j(elt).closest('.test_name_'+index);
  parent.find(".test_collapseLink_"+ index).hide();
  parent.find(".test_expandLink_"+ index).show();
  parent.next(".tests_viewer .test_message_"+ index).hide();
}

/* Source decoration functions */
function highlight_usages(event){
  var isAlreadyHighlighted = false;
  var selectedElementClasses = $j(this).attr("class").split(" ");
  if(selectedElementClasses.indexOf("highlighted") != -1) {
    isAlreadyHighlighted = true;
  }
  $j("#" + event.data.id + " span.highlighted").removeClass("highlighted");

  if(!isAlreadyHighlighted) {
    var selectedClass = selectedElementClasses[0];
    $j("#" + event.data.id + " span." + selectedClass).addClass("highlighted");
  }
}