import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class ArchitectureCheckToSqlFromUI {

  ResultSet result;

  Connection connection;

  public ArchitectureCheckToSqlFromUI(Statement statement, String requete) {
    try {
      connection = statement.getConnection();
      result = statement.executeQuery(requete);
    } catch (SQLException sql) {
      sql.printStackTrace();
    }
  }
}
