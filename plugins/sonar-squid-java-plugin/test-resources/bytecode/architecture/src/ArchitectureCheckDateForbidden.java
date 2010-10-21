import java.sql.Date;
import java.util.Calendar;

public class ArchitectureCheckDateForbidden {

  public ArchitectureCheckDateForbidden() {
    Date dateSql = new Date(200000);
    java.util.Date dateUtil = Calendar.getInstance().getTime();
    long time = dateUtil.getTime();
  }
}
