package org.sonar.core.issue.workflow;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SetAssigneeTest {
  @Test
  public void assign() throws Exception {
    SetAssignee function = new SetAssignee("eric");
    Function.Context context = mock(Function.Context.class);
    function.execute(context);
    verify(context, times(1)).setAssignee("eric");
  }

  @Test
  public void unassign() throws Exception {
    Function.Context context = mock(Function.Context.class);
    SetAssignee.UNASSIGN.execute(context);
    verify(context, times(1)).setAssignee(null);
  }
}
