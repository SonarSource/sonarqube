/*
 Functions used in resource viewers
 */

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
function highlightUsages(event){
  var isAlreadyHighlighted = false;
  var selectedElementClasses = $j(this).attr("class").split(" ");
  if(selectedElementClasses.indexOf("highlighted") !== -1) {
    isAlreadyHighlighted = true;
  }
  $j("#" + event.data.id + " span.highlighted").removeClass("highlighted");

  if(!isAlreadyHighlighted) {
    var selectedClass = selectedElementClasses[0];
    $j("#" + event.data.id + " span." + selectedClass).addClass("highlighted");
  }
}
