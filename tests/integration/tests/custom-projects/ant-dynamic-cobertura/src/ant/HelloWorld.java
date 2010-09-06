package ant;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import java.util.Vector;

public class HelloWorld {
    static Logger logger = Logger.getLogger(HelloWorld.class);

    public static void main(String[] args) {
        BasicConfigurator.configure();
        logger.info("Hello World");          // the old SysO-statement
        
        String temp = new String();
        Vector<String> v = new Vector<String>();
    }

    public Object clone() {
	    return new HelloWorld();
    }
    public boolean equals(Object o) {
	  return true;
    }

    public boolean Equals(Object o) {
	  return true;
    }
}