/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jdbc_adapter;

import java.sql.Connection;
import java.sql.SQLException;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author nicksieger
 */
public interface SQLBlock {
    IRubyObject call(Connection c) throws SQLException;
}
