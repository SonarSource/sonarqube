/* Open form for most common actions like comment, assign or plan */
function issueForm(actionType, elt) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var issueKey = issueElt.attr('data-issue-key');
  var actionsElt = issueElt.find('.code-issue-actions');
  var formElt = issueElt.find('.code-issue-form');

  actionsElt.addClass('hidden');
  formElt.html("<img src='" + baseUrl + "/images/loading-small.gif'>").removeClass('hidden');

  $j.ajax(baseUrl + "/issue/action_form/" + actionType + "?issue=" + issueKey)
    .done(function (msg) {
      formElt.html(msg);
      var focusField = formElt.find('[autofocus]');
      if (focusField != null) {
        focusField.focus();
      }
    })
    .fail(function (jqXHR, textStatus) {
      alert(textStatus);
    });
  return false;
}

/* Close forms opened through the method issueForm()  */
function closeIssueForm(elt) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var actionsElt = issueElt.find('.code-issue-actions');
  var formElt = issueElt.find('.code-issue-form');

  formElt.addClass('hidden');
  actionsElt.removeClass('hidden');
  return false;
}

/* Raise a Javascript event for Eclipse Web View */
function notifyIssueChange(issueKey) {
  $j(document).trigger('sonar.issue.updated', [issueKey]);
}

/* Submit forms opened through the method issueForm() */
function submitIssueForm(elt) {
  var formElt = $j(elt).closest('form');
  formElt.find('.loading').removeClass('hidden');
  formElt.find(':submit').prop('disabled', true);
  var issueElt = formElt.closest('[data-issue-key]');
  var issueKey = issueElt.attr('data-issue-key');

  $j.ajax({
      type: "POST",
      url: baseUrl + '/issue/do_action',
      data: formElt.serialize()}
  ).success(function (htmlResponse) {
      var replaced = $j(htmlResponse);
      issueElt.replaceWith(replaced);
      notifyIssueChange(issueKey);
    }
  ).fail(function (jqXHR, textStatus) {
      closeIssueForm(elt);
      issueElt.find('.code-issue-actions').replaceWith(jqXHR.responseText);
    });
  return false;
}

function doIssueAction(elt, action, parameters) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var issueKey = issueElt.attr('data-issue-key');

  issueElt.find('.code-issue-actions').html("<img src='" + baseUrl + "/images/loading.gif'>");
  parameters['issue'] = issueKey;

  $j.ajax({
      type: "POST",
      url: baseUrl + '/issue/do_action/' + action,
      data: parameters
    }
  ).success(function (htmlResponse) {
      var replaced = $j(htmlResponse);
      issueElt.replaceWith(replaced);
      notifyIssueChange(issueKey);
    }
  ).fail(function (jqXHR, textStatus) {
      issueElt.find('.code-issue-actions').replaceWith(jqXHR.responseText);
    });
  return false;
}

// Used for actions defined by plugins
function doPluginIssueAction(elt, action) {
  var parameters = {};
  return doIssueAction(elt, action, parameters);
}

function assignIssueToMe(elt) {
  var parameters = {'me': true};
  return doIssueAction(elt, 'assign', parameters);
}

function doIssueTransition(elt, transition) {
  var parameters = {'transition': transition};
  return doIssueAction(elt, 'transition', parameters);
}

function deleteIssueComment(elt, confirmMsg) {
  var commentElt = $j(elt).closest("[data-comment-key]");
  var commentKey = commentElt.attr('data-comment-key');
  var issueElt = commentElt.closest('[data-issue-key]');
  if (confirm(confirmMsg)) {
    $j.ajax({
      type: "POST",
      url: baseUrl + "/issue/delete_comment?id=" + commentKey,
      success: function (htmlResponse) {
        issueElt.replaceWith($j(htmlResponse));
      }
    });
  }
  return false;
}

function formEditIssueComment(elt) {
  var commentElt = $j(elt).closest("[data-comment-key]");
  var commentKey = commentElt.attr('data-comment-key');
  var issueElt = commentElt.closest('[data-issue-key]');

  issueElt.find('.code-issue-actions').addClass('hidden');
  commentElt.html("<img src='" + baseUrl + "/images/loading.gif'>");

  $j.get(baseUrl + "/issue/edit_comment_form/" + commentKey, function (html) {
    commentElt.html(html);
  });
  return false;
}

function doEditIssueComment(elt) {
  var formElt = $j(elt).closest('form');
  var issueElt = formElt.closest('[data-issue-key]');
  var issueKey = issueElt.attr('data-issue-key');
  $j.ajax({
    type: "POST",
    url: baseUrl + "/issue/edit_comment",
    data: formElt.serialize(),
    success: function (htmlResponse) {
      var replaced = $j(htmlResponse);
      issueElt.replaceWith(replaced);
      notifyIssueChange(issueKey);
    },
    error: function (jqXHR, textStatus) {
      closeIssueForm(elt);
      var commentElt = formElt.closest('[data-comment-key]');
      commentElt.replaceWith(jqXHR.responseText);
    }
  });
  return false;
}

function refreshIssue(elt) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var issueKey = issueElt.attr('data-issue-key');
  $j.get(baseUrl + "/issue/show/" + issueKey + "?only_detail=true", function (html) {
    var replaced = $j(html);
    issueElt.replaceWith(replaced);
  });
  return false;
}

/* Open form for creating a manual issue */
function openCIF(elt, componentId, line) {
  // TODO check if form is already displayed (by using form id)
  $j.get(baseUrl + "/issue/create_form?component=" + componentId + "&line=" + line, function (html) {
    $j(elt).closest('tr').find('td.line').append($j(html));
  });
  return false;
}

/* Close the form used for creating a manual issue */
function closeCreateIssueForm(elt) {
  $j(elt).closest('.code-issue-create-form').remove();
  return false;
}

/* Create a manual issue */
function submitCreateIssueForm(elt) {
  var formElt = $j(elt).closest('form');
  var loadingElt = formElt.find('.loading');

  loadingElt.removeClass('hidden');
  $j.ajax({
      type: "POST",
      url: baseUrl + '/issue/create',
      data: formElt.serialize()}
  ).success(function (html) {
      var replaced = $j(html);
      formElt.replaceWith(replaced);
    }
  ).error(function (jqXHR, textStatus, errorThrown) {
      var errorsElt = formElt.find('.code-issue-errors');
      errorsElt.html(jqXHR.responseText);
      errorsElt.removeClass('hidden');
    }
  ).always(function () {
      loadingElt.addClass('hidden');
    });
  return false;
}

function toggleIssueRule(elt) {
  var issueElt = $j(elt).closest('[data-issue-rule]');
  var ruleElt = issueElt.find('.issue-rule');
  if (ruleElt.is(':visible')) {
    ruleElt.slideUp('fast');
  } else {
    issueElt.find('.issue-changelog').slideUp('fast');
    var ruleKey = issueElt.attr('data-issue-rule');
    $j.get(baseUrl + "/issue/rule/" + ruleKey, function (html) {
      ruleElt.html(html);
      ruleElt.slideDown('fast');

      // re-enable the links opening modal popups
      ruleElt.find('.open-modal').modal();
    });
  }
  return false;
}

function toggleIssueChangelog(elt) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var changelogElt = issueElt.find('.issue-changelog');
  if (changelogElt.is(':visible')) {
    changelogElt.slideUp('fast');
  } else {
    issueElt.find('.issue-rule').slideUp('fast');
    var issueKey = issueElt.attr('data-issue-key');
    $j.get(baseUrl + "/issue/changelog/" + issueKey, function (html) {
      changelogElt.html(html);
      changelogElt.slideDown('fast');
    });
  }
  return false;
}

function openIssueRulePopup(elt) {
  var issueElt = $j(elt).closest('[data-issue-rule]');
  var ruleKey = issueElt.attr('data-issue-rule');
  openPopup(baseUrl + "/rules/show/" + ruleKey + "?layout=false", 'rule');
  return false;
}

function openIssuePopup(elt) {
  var issueElt = $j(elt).closest('[data-issue-key]');
  var issueKey = issueElt.attr('data-issue-key');
  openPopup(baseUrl + "/issue/show/" + issueKey + "?layout=false", 'issue');
  return false;
}