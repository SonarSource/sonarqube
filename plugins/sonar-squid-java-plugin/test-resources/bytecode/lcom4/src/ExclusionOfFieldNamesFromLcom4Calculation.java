

import java.util.logging.Level;
import java.util.logging.Logger;

public class ExclusionOfFieldNamesFromLcom4Calculation {
  
  private final Logger LOG = Logger.getLogger("log");
  private boolean killAccessorBlockA;
  private boolean killAccessorBlockB;
  
  public void firstMethodBlockA(){
    secondMethodBlockA();
    LOG.log(Level.INFO, "a message");
  }
  
  public void secondMethodBlockA(){
    LOG.log(Level.INFO, "a message");
    killAccessorBlockA = true;
  }
  
  public void firstMethodBlockB(){
    secondMethodBlockB();
    LOG.log(Level.INFO, "a message");
  }
  
  public void secondMethodBlockB(){
    LOG.log(Level.INFO, "a message");
    killAccessorBlockB = true;
  }
  
}
