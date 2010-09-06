/***** BEGIN LICENSE BLOCK *****
 * Copyright (c) 2006-2009 Nick Sieger <nick@nicksieger.com>
 * Copyright (c) 2006-2007 Ola Bini <ola.bini@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ***** END LICENSE BLOCK *****/

package jdbc_adapter;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.StringReader;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObjectAdapter;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.BasicLibraryService;
import org.jruby.util.ByteList;

public class JdbcAdapterInternalService implements BasicLibraryService {
    private static RubyObjectAdapter rubyApi;

    public boolean basicLoad(final Ruby runtime) throws IOException {
        RubyModule jdbcConnection = ((RubyModule)(runtime.getModule("ActiveRecord").getConstant("ConnectionAdapters"))).
            defineClassUnder("JdbcConnection",runtime.getObject(),runtime.getObject().getAllocator());
        jdbcConnection.defineAnnotatedMethods(JdbcAdapterInternalService.class);
        RubyModule jdbcSpec = runtime.getOrCreateModule("JdbcSpec");

        rubyApi = JavaEmbedUtils.newObjectAdapter();
        JdbcMySQLSpec.load(jdbcSpec);
        JdbcDerbySpec.load(jdbcSpec, rubyApi);
        return true;
    }

    private static int whitespace(int p, final int pend, ByteList bl) {
        while(p < pend) {
            switch(bl.bytes[p]) {
            case ' ':
            case '\n':
            case '\r':
            case '\t':
                p++;
                break;
            default:
                return p;
            }
        }
        return p;
    }

    @JRubyMethod(name = "insert?", required = 1, meta = true)
    public static IRubyObject insert_p(IRubyObject recv, IRubyObject _sql) {
        ByteList bl = rubyApi.convertToRubyString(_sql).getByteList();

        int p = bl.begin;
        int pend = p + bl.realSize;

        p = whitespace(p, pend, bl);

        if(pend - p >= 6) {
            switch(bl.bytes[p++]) {
            case 'i':
            case 'I':
                switch(bl.bytes[p++]) {
                case 'n':
                case 'N':
                    switch(bl.bytes[p++]) {
                    case 's':
                    case 'S':
                        switch(bl.bytes[p++]) {
                        case 'e':
                        case 'E':
                            switch(bl.bytes[p++]) {
                            case 'r':
                            case 'R':
                                switch(bl.bytes[p++]) {
                                case 't':
                                case 'T':
                                    return recv.getRuntime().getTrue();
                                }
                            }
                        }
                    }
                }
            }
        }
        return recv.getRuntime().getFalse();
    }

    @JRubyMethod(name = "select?", required = 1, meta = true)
    public static IRubyObject select_p(IRubyObject recv, IRubyObject _sql) {
        ByteList bl = rubyApi.convertToRubyString(_sql).getByteList();

        int p = bl.begin;
        int pend = p + bl.realSize;

        p = whitespace(p, pend, bl);

        if(pend - p >= 6) {
            if(bl.bytes[p] == '(') {
                p++;
                p = whitespace(p, pend, bl);
            }
            if(pend - p >= 6) {
                switch(bl.bytes[p++]) {
                case 's':
                case 'S':
                    switch(bl.bytes[p++]) {
                    case 'e':
                    case 'E':
                        switch(bl.bytes[p++]) {
                        case 'l':
                        case 'L':
                            switch(bl.bytes[p++]) {
                            case 'e':
                            case 'E':
                                switch(bl.bytes[p++]) {
                                case 'c':
                                case 'C':
                                    switch(bl.bytes[p++]) {
                                    case 't':
                                    case 'T':
                                        return recv.getRuntime().getTrue();
                                    }
                                }
                            }
                        }
                    case 'h':
                    case 'H':
                        switch(bl.bytes[p++]) {
                        case 'o':
                        case 'O':
                            switch(bl.bytes[p++]) {
                            case 'w':
                            case 'W':
                                return recv.getRuntime().getTrue();
                            }
                        }
                    }
                }
            }
        }
        return recv.getRuntime().getFalse();
    }

    @JRubyMethod(name = "connection")
    public static IRubyObject connection(IRubyObject recv) {
        Connection c = getConnection(recv);
        if (c == null) {
            reconnect(recv);
        }
        return rubyApi.getInstanceVariable(recv, "@connection");
    }

    @JRubyMethod(name = "disconnect!")
    public static IRubyObject disconnect(IRubyObject recv) {
        setConnection(recv, null);
        return recv;
    }

    @JRubyMethod(name = "reconnect!")
    public static IRubyObject reconnect(IRubyObject recv) {
        setConnection(recv, getConnectionFactory(recv).newConnection());
        return recv;
    }

    @JRubyMethod(name = "with_connection_retry_guard", frame = true)
    public static IRubyObject with_connection_retry_guard(final IRubyObject recv, final Block block) {
        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                return block.call(recv.getRuntime().getCurrentContext(), new IRubyObject[] {
                    wrappedConnection(recv, c)
                });
            }
        });
    }

    private static IRubyObject withConnectionAndRetry(IRubyObject recv, SQLBlock block) {
        int tries = 1;
        int i = 0;
        Throwable toWrap = null;
        boolean autoCommit = false;
        while (i < tries) {
            Connection c = getConnection(recv, true);
            try {
                autoCommit = c.getAutoCommit();
                return block.call(c);
            } catch (Exception e) {
                toWrap = e;
                while (toWrap.getCause() != null && toWrap.getCause() != toWrap) {
                    toWrap = toWrap.getCause();
                }
                i++;
                if (autoCommit) {
                    if (i == 1) {
                        tries = (int) rubyApi.convertToRubyInteger(config_value(recv, "retry_count")).getLongValue();
                        if (tries <= 0) {
                            tries = 1;
                        }
                    }
                    if (isConnectionBroken(recv, c)) {
                        reconnect(recv);
                    } else {
                        throw wrap(recv, toWrap);
                    }
                }
            }
        }
        throw wrap(recv, toWrap);
    }

    private static SQLBlock tableLookupBlock(final Ruby runtime,
            final String catalog, final String schemapat,
            final String tablepat, final String[] types) {
        return new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                ResultSet rs = null;
                try {
                    DatabaseMetaData metadata = c.getMetaData();
                    String clzName = metadata.getClass().getName().toLowerCase();
                    boolean isOracle = clzName.indexOf("oracle") != -1 || clzName.indexOf("oci") != -1;

                    String realschema = schemapat;
                    String realtablepat = tablepat;

                    if(metadata.storesUpperCaseIdentifiers()) {
                        if (realschema != null) realschema = realschema.toUpperCase();
                        if (realtablepat != null) realtablepat = realtablepat.toUpperCase();
                    } else if(metadata.storesLowerCaseIdentifiers()) {
                        if (null != realschema) realschema = realschema.toLowerCase();
                        if (realtablepat != null) realtablepat = realtablepat.toLowerCase();
                    }

                    if (realschema == null && isOracle) {
                        ResultSet schemas = metadata.getSchemas();
                        String username = metadata.getUserName();
                        while (schemas.next()) {
                            if (schemas.getString(1).equalsIgnoreCase(username)) {
                                realschema = schemas.getString(1);
                                break;
                            }
                        }
                        schemas.close();
                    }
                    rs = metadata.getTables(catalog, realschema, realtablepat, types);
                    List arr = new ArrayList();
                    while (rs.next()) {
                        String name = rs.getString(3).toLowerCase();
                        // Handle stupid Oracle 10g RecycleBin feature
                        if (!isOracle || !name.startsWith("bin$")) {
                            arr.add(RubyString.newUnicodeString(runtime, name));
                        }
                    }
                    return runtime.newArray(arr);
                } finally {
                    try { rs.close(); } catch (Exception e) { }
                }
            }
        };
    }

    @JRubyMethod(name = "tables", rest = true)
    public static IRubyObject tables(final IRubyObject recv, IRubyObject[] args) {
        final Ruby runtime     = recv.getRuntime();
        final String catalog   = getCatalog(args);
        final String schemapat = getSchemaPattern(args);
        final String tablepat  = getTablePattern(args);
        final String[] types   = getTypes(args);
        return withConnectionAndRetry(recv, tableLookupBlock(runtime, catalog,
                schemapat, tablepat, types));
    }

    private static String getCatalog(IRubyObject[] args) {
        if (args != null && args.length > 0) {
            return convertToStringOrNull(args[0]);
        }
        return null;
    }

    private static String getSchemaPattern(IRubyObject[] args) {
        if (args != null && args.length > 1) {
            return convertToStringOrNull(args[1]);
        }
        return null;
    }

    private static String getTablePattern(IRubyObject[] args) {
        if (args != null && args.length > 2) {
            return convertToStringOrNull(args[2]);
        }
        return null;
    }

    private static String[] getTypes(IRubyObject[] args) {
        String[] types = new String[]{"TABLE"};
        if (args != null && args.length > 3) {
            IRubyObject typearr = args[3];
            if (typearr instanceof RubyArray) {
                IRubyObject[] arr = rubyApi.convertToJavaArray(typearr);
                types = new String[arr.length];
                for (int i = 0; i < types.length; i++) {
                    types[i] = arr[i].toString();
                }
            } else {
                types = new String[]{types.toString()};
            }
        }
        return types;
    }

    @JRubyMethod(name = "native_database_types")
    public static IRubyObject native_database_types(IRubyObject recv) {
        return rubyApi.getInstanceVariable(recv, "@tps");
    }

    @JRubyMethod(name = "set_native_database_types")
    public static IRubyObject set_native_database_types(IRubyObject recv) throws SQLException, IOException {
        Ruby runtime = recv.getRuntime();
        IRubyObject types = unmarshal_result_downcase(recv, getConnection(recv, true).getMetaData().getTypeInfo());
        IRubyObject typeConverter = ((RubyModule) (runtime.getModule("ActiveRecord").getConstant("ConnectionAdapters"))).getConstant("JdbcTypeConverter");
        IRubyObject value = rubyApi.callMethod(rubyApi.callMethod(typeConverter, "new", types), "choose_best_types");
        rubyApi.setInstanceVariable(recv, "@native_types", value);
        return runtime.getNil();
    }

    @JRubyMethod(name = "database_name")
    public static IRubyObject database_name(IRubyObject recv) throws SQLException {
        String name = getConnection(recv, true).getCatalog();
        if(null == name) {
            name = getConnection(recv, true).getMetaData().getUserName();
            if(null == name) {
                name = "db1";
            }
        }
        return recv.getRuntime().newString(name);
    }

    @JRubyMethod(name = "begin")
    public static IRubyObject begin(IRubyObject recv) throws SQLException {
        getConnection(recv, true).setAutoCommit(false);
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "commit")
    public static IRubyObject commit(IRubyObject recv) throws SQLException {
        Connection c = getConnection(recv, true);
        if (!c.getAutoCommit()) {
            try {
                c.commit();
            } finally {
                c.setAutoCommit(true);
            }
        }
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "rollback")
    public static IRubyObject rollback(IRubyObject recv) throws SQLException {
        Connection c = getConnection(recv, true);
        if (!c.getAutoCommit()) {
            try {
                c.rollback();
            } finally {
                c.setAutoCommit(true);
            }
        }
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = {"columns", "columns_internal"}, required = 1, optional = 2)
    public static IRubyObject columns_internal(final IRubyObject recv, final IRubyObject[] args) throws SQLException, IOException {
        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                ResultSet results = null;
                try {
                    String table_name = rubyApi.convertToRubyString(args[0]).getUnicodeValue();
                    String schemaName = null;

                    int index = table_name.indexOf(".");
                    if(index != -1) {
                        schemaName = table_name.substring(0, index);
                        table_name = table_name.substring(index + 1);
                    }

                    DatabaseMetaData metadata = c.getMetaData();
                    String clzName = metadata.getClass().getName().toLowerCase();
                    boolean isDerby = clzName.indexOf("derby") != -1;
                    boolean isOracle = clzName.indexOf("oracle") != -1 || clzName.indexOf("oci") != -1;

                    if(args.length>2) {
                        schemaName = args[2].toString();
                    }

                    if(metadata.storesUpperCaseIdentifiers()) {
                        if (null != schemaName) schemaName = schemaName.toUpperCase();
                        table_name = table_name.toUpperCase();
                    } else if(metadata.storesLowerCaseIdentifiers()) {
                        if (null != schemaName) schemaName = schemaName.toLowerCase();
                        table_name = table_name.toLowerCase();
                    }

                    if(schemaName == null && (isDerby || isOracle)) {
                        ResultSet schemas = metadata.getSchemas();
                        String username = metadata.getUserName();
                        while(schemas.next()) {
                            if(schemas.getString(1).equalsIgnoreCase(username)) {
                                schemaName = schemas.getString(1);
                                break;
                            }
                        }
                        schemas.close();
                    }

                    RubyArray matchingTables = (RubyArray) tableLookupBlock(recv.getRuntime(),
                                                                            c.getCatalog(), schemaName, table_name, new String[]{"TABLE","VIEW"}).call(c);
                    if (matchingTables.isEmpty()) {
                        throw new SQLException("Table " + table_name + " does not exist");
                    }

                    results = metadata.getColumns(c.getCatalog(),schemaName,table_name,null);
                    return unmarshal_columns(recv, metadata, results);
                } finally {
                    try { if (results != null) results.close(); } catch (SQLException sqx) {}
                }
            }
        });
    }

    private static final java.util.regex.Pattern HAS_SMALL = java.util.regex.Pattern.compile("[a-z]");
    private static IRubyObject unmarshal_columns(IRubyObject recv, DatabaseMetaData metadata, ResultSet rs) throws SQLException {
        try {
            List columns = new ArrayList();
            String clzName = metadata.getClass().getName().toLowerCase();
            boolean isDerby = clzName.indexOf("derby") != -1;
            boolean isOracle = clzName.indexOf("oracle") != -1 || clzName.indexOf("oci") != -1;
            Ruby runtime = recv.getRuntime();

            IRubyObject adapter = rubyApi.callMethod(recv, "adapter");
            RubyHash tps = (RubyHash) rubyApi.callMethod(adapter, "native_database_types");

            IRubyObject jdbcCol = ((RubyModule)(runtime.getModule("ActiveRecord").getConstant("ConnectionAdapters"))).getConstant("JdbcColumn");

            while(rs.next()) {
                String column_name = rs.getString(4);
                if(metadata.storesUpperCaseIdentifiers() && !HAS_SMALL.matcher(column_name).find()) {
                    column_name = column_name.toLowerCase();
                }

                String prec = rs.getString(7);
                String scal = rs.getString(9);
                int precision = -1;
                int scale = -1;
                if(prec != null) {
                    precision = Integer.parseInt(prec);
                    if(scal != null) {
                        scale = Integer.parseInt(scal);
                    }
                    else if(isOracle && rs.getInt(5) == java.sql.Types.DECIMAL) { // NUMBER type in Oracle
                        prec = null;
                    }
                }
                String type = rs.getString(6);
                if(prec != null && precision > 0) {
                    type += "(" + precision;
                    if(scal != null && scale > 0) {
                        type += "," + scale;
                    }
                    type += ")";
                }
                String def = rs.getString(13);
                IRubyObject _def;
                if(def == null || (isOracle && def.toLowerCase().trim().equals("null"))) {
                    _def = runtime.getNil();
                } else {
                    if(isOracle) {
                        def = def.trim();
                    }
                    if((isDerby || isOracle) && def.length() > 0 && def.charAt(0) == '\'') {
                        def = def.substring(1, def.length()-1);
                    }
                    _def = RubyString.newUnicodeString(runtime, def);
                }
                IRubyObject config = rubyApi.getInstanceVariable(recv, "@config");
                IRubyObject c = rubyApi.callMethod(jdbcCol, "new",
                        new IRubyObject[]{
                                                       config, RubyString.newUnicodeString(runtime, column_name),
                                                       _def, RubyString.newUnicodeString(runtime, type),
                            runtime.newBoolean(!rs.getString(18).trim().equals("NO"))
                        });
                columns.add(c);

                IRubyObject tp = (IRubyObject)tps.fastARef(rubyApi.callMethod(c,"type"));
                if(tp != null && !tp.isNil() && rubyApi.callMethod(tp, "[]", runtime.newSymbol("limit")).isNil()) {
                    rubyApi.callMethod(c, "limit=", runtime.getNil());
                    if(!rubyApi.callMethod(c, "type").equals(runtime.newSymbol("decimal"))) {
                        rubyApi.callMethod(c, "precision=", runtime.getNil());
                    }
                }
            }
            return runtime.newArray(columns);
        } finally {
            try {
                rs.close();
            } catch(Exception e) {}
        }
    }

    @JRubyMethod(name = "primary_keys", required = 1)
    public static IRubyObject primary_keys(final IRubyObject recv, final IRubyObject _table_name) throws SQLException {
        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                DatabaseMetaData metadata = c.getMetaData();
                String table_name = _table_name.toString();
                if (metadata.storesUpperCaseIdentifiers()) {
                    table_name = table_name.toUpperCase();
                } else if (metadata.storesLowerCaseIdentifiers()) {
                    table_name = table_name.toLowerCase();
                }
                ResultSet result_set = metadata.getPrimaryKeys(null, null, table_name);
                List keyNames = new ArrayList();
                Ruby runtime = recv.getRuntime();
                while (result_set.next()) {
                    String s1 = result_set.getString(4);
                    if (metadata.storesUpperCaseIdentifiers() && !HAS_SMALL.matcher(s1).find()) {
                        s1 = s1.toLowerCase();
                    }
                    keyNames.add(RubyString.newUnicodeString(runtime,s1));
                }

                try {
                    result_set.close();
                } catch (Exception e) {
                }

                return runtime.newArray(keyNames);
            }
        });
    }

    @JRubyMethod(name = "execute_id_insert", required = 2)
    public static IRubyObject execute_id_insert(IRubyObject recv, final IRubyObject sql, final IRubyObject id) throws SQLException {
        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement(rubyApi.convertToRubyString(sql).getUnicodeValue());
                try {
                    ps.setLong(1, RubyNumeric.fix2long(id));
                    ps.executeUpdate();
                } finally {
                    ps.close();
                }
                return id;
            }
        });
    }

    @JRubyMethod(name = "execute_update", required = 1)
    public static IRubyObject execute_update(final IRubyObject recv, final IRubyObject sql) throws SQLException {
        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                Statement stmt = null;
                try {
                    stmt = c.createStatement();
                    return recv.getRuntime().newFixnum((long)stmt.executeUpdate(rubyApi.convertToRubyString(sql).getUnicodeValue()));
                } finally {
                    if (null != stmt) {
                        try {
                            stmt.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        });
    }

    @JRubyMethod(name = "execute_query", rest = true)
    public static IRubyObject execute_query(final IRubyObject recv, IRubyObject[] args) throws SQLException, IOException {
        final IRubyObject sql = args[0];
        final int maxrows;

        if (args.length > 1) {
            maxrows = RubyNumeric.fix2int(args[1]);
        } else {
            maxrows = 0;
        }

        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                Statement stmt = null;
                try {
                    stmt = c.createStatement();
                    stmt.setMaxRows(maxrows);
                    return unmarshal_result(recv, stmt.executeQuery(rubyApi.convertToRubyString(sql).getUnicodeValue()));
                } finally {
                    if (null != stmt) {
                        try {
                            stmt.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        });
    }

    @JRubyMethod(name = "execute_insert", required = 1)
    public static IRubyObject execute_insert(final IRubyObject recv, final IRubyObject sql) throws SQLException {
        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                Statement stmt = null;
                try {
                    stmt = c.createStatement();
                    stmt.executeUpdate(rubyApi.convertToRubyString(sql).getUnicodeValue(), Statement.RETURN_GENERATED_KEYS);
                    return unmarshal_id_result(recv.getRuntime(), stmt.getGeneratedKeys());
                } finally {
                    if (null != stmt) {
                        try {
                            stmt.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        });
    }

    public static IRubyObject unmarshal_result_downcase(IRubyObject recv, ResultSet rs) throws SQLException, IOException {
        List results = new ArrayList();
        Ruby runtime = recv.getRuntime();
        try {
            ResultSetMetaData metadata = rs.getMetaData();
            int col_count = metadata.getColumnCount();
            IRubyObject[] col_names = new IRubyObject[col_count];
            int[] col_types = new int[col_count];
            int[] col_scale = new int[col_count];

            for(int i=0;i<col_count;i++) {
                col_names[i] = RubyString.newUnicodeString(runtime, metadata.getColumnLabel(i+1).toLowerCase());
                col_types[i] = metadata.getColumnType(i+1);
                col_scale[i] = metadata.getScale(i+1);
            }

            while(rs.next()) {
                RubyHash row = RubyHash.newHash(runtime);
                for(int i=0;i<col_count;i++) {
                    rubyApi.callMethod(row, "[]=", new IRubyObject[] {
                        col_names[i], jdbc_to_ruby(runtime, i+1, col_types[i], col_scale[i], rs)
                    });
                }
                results.add(row);
            }
        } finally {
            try {
                rs.close();
            } catch(Exception e) {}
        }

        return runtime.newArray(results);
    }

    public static IRubyObject unmarshal_result(IRubyObject recv, ResultSet rs) throws SQLException {
        Ruby runtime = recv.getRuntime();
        List results = new ArrayList();
        try {
            ResultSetMetaData metadata = rs.getMetaData();
            boolean storesUpper = rs.getStatement().getConnection().getMetaData().storesUpperCaseIdentifiers();
            int col_count = metadata.getColumnCount();
            IRubyObject[] col_names = new IRubyObject[col_count];
            int[] col_types = new int[col_count];
            int[] col_scale = new int[col_count];

            for(int i=0;i<col_count;i++) {
                String s1 = metadata.getColumnLabel(i+1);
                if(storesUpper && !HAS_SMALL.matcher(s1).find()) {
                    s1 = s1.toLowerCase();
                }
                col_names[i] = RubyString.newUnicodeString(runtime, s1);
                col_types[i] = metadata.getColumnType(i+1);
                col_scale[i] = metadata.getScale(i+1);
            }

            while(rs.next()) {
                RubyHash row = RubyHash.newHash(runtime);
                for(int i=0;i<col_count;i++) {
                    rubyApi.callMethod(row, "[]=", new IRubyObject[] {
                        col_names[i], jdbc_to_ruby(runtime, i+1, col_types[i], col_scale[i], rs)
                    });
                }
                results.add(row);
            }
        } finally {
            try {
                rs.close();
            } catch(Exception e) {}
        }
        return runtime.newArray(results);
    }

    @JRubyMethod(name = "unmarshal_result", required = 1)
    public static IRubyObject unmarshal_result(IRubyObject recv, IRubyObject resultset, Block row_filter) throws SQLException, IOException {
        Ruby runtime = recv.getRuntime();
        ResultSet rs = intoResultSet(resultset);
        List results = new ArrayList();
        try {
            ResultSetMetaData metadata = rs.getMetaData();
            int col_count = metadata.getColumnCount();
            IRubyObject[] col_names = new IRubyObject[col_count];
            int[] col_types = new int[col_count];
            int[] col_scale = new int[col_count];

            for (int i=0;i<col_count;i++) {
                col_names[i] = RubyString.newUnicodeString(runtime, metadata.getColumnLabel(i+1));
                col_types[i] = metadata.getColumnType(i+1);
                col_scale[i] = metadata.getScale(i+1);
            }

            if (row_filter.isGiven()) {
                while (rs.next()) {
                    if (row_filter.yield(runtime.getCurrentContext(),resultset).isTrue()) {
                        RubyHash row = RubyHash.newHash(runtime);
                        for (int i=0;i<col_count;i++) {
                            rubyApi.callMethod(row, "[]=", new IRubyObject[] {
                                col_names[i], jdbc_to_ruby(runtime, i+1, col_types[i], col_scale[i], rs)
                            });
                        }
                        results.add(row);
                    }
                }
            } else {
                while (rs.next()) {
                    RubyHash row = RubyHash.newHash(runtime);
                    for (int i=0;i<col_count;i++) {
                        rubyApi.callMethod(row, "[]=", new IRubyObject[] {
                            col_names[i], jdbc_to_ruby(runtime, i+1, col_types[i], col_scale[i], rs)
                        });
                    }
                    results.add(row);
                }
            }

        } finally {
            try {
                rs.close();
            } catch(Exception e) {}
        }

        return runtime.newArray(results);
    }

    private static IRubyObject jdbc_to_ruby(Ruby runtime, int row, int type, int scale, ResultSet rs) throws SQLException {
        try {
            int n;
            switch (type) {
                case Types.BINARY:
                case Types.BLOB:
                case Types.LONGVARBINARY:
                case Types.VARBINARY:
                    InputStream is = rs.getBinaryStream(row);
                    if (is == null || rs.wasNull()) {
                        return runtime.getNil();
                    }
                    ByteList str = new ByteList(2048);
                    byte[] buf = new byte[2048];

                    while ((n = is.read(buf)) != -1) {
                        str.append(buf, 0, n);
                    }
                    is.close();

                    return runtime.newString(str);
                case Types.LONGVARCHAR:
                case Types.CLOB:
                    Reader rss = rs.getCharacterStream(row);
                    if (rss == null || rs.wasNull()) {
                        return runtime.getNil();
                    }
                    StringBuffer str2 = new StringBuffer(2048);
                    char[] cuf = new char[2048];
                    while ((n = rss.read(cuf)) != -1) {
                        str2.append(cuf, 0, n);
                    }
                    rss.close();
                    return RubyString.newUnicodeString(runtime, str2.toString());
                case Types.TIMESTAMP:
                    Timestamp time = rs.getTimestamp(row);
                    if (time == null || rs.wasNull()) {
                        return runtime.getNil();
                    }
                    String sttr = time.toString();
                    if (sttr.endsWith(" 00:00:00.0")) {
                        sttr = sttr.substring(0, sttr.length() - (" 00:00:00.0".length()));
                    }
                    return RubyString.newUnicodeString(runtime, sttr);
                default:
                    String vs = rs.getString(row);
                    if (vs == null || rs.wasNull()) {
                        return runtime.getNil();
                    }

                    return RubyString.newUnicodeString(runtime, vs);
            }
        } catch (IOException ioe) {
            throw (SQLException) new SQLException(ioe.getMessage()).initCause(ioe);
        }
    }

    public static IRubyObject unmarshal_id_result(Ruby runtime, ResultSet rs) throws SQLException {
        try {
            if(rs.next()) {
                if(rs.getMetaData().getColumnCount() > 0) {
                    return runtime.newFixnum(rs.getLong(1));
                }
            }
            return runtime.getNil();
        } finally {
            try {
                rs.close();
            } catch(Exception e) {}
        }
    }

    private static String convertToStringOrNull(IRubyObject obj) {
        if (obj.isNil()) {
            return null;
        }
        return obj.toString();
    }

    private static int getTypeValueFor(Ruby runtime, IRubyObject type) throws SQLException {
        if(!(type instanceof RubySymbol)) {
            type = rubyApi.callMethod(type, "class");
        }
        if(type == runtime.newSymbol("string")) {
            return Types.VARCHAR;
        } else if(type == runtime.newSymbol("text")) {
            return Types.CLOB;
        } else if(type == runtime.newSymbol("integer")) {
            return Types.INTEGER;
        } else if(type == runtime.newSymbol("decimal")) {
            return Types.DECIMAL;
        } else if(type == runtime.newSymbol("float")) {
            return Types.FLOAT;
        } else if(type == runtime.newSymbol("datetime")) {
            return Types.TIMESTAMP;
        } else if(type == runtime.newSymbol("timestamp")) {
            return Types.TIMESTAMP;
        } else if(type == runtime.newSymbol("time")) {
            return Types.TIME;
        } else if(type == runtime.newSymbol("date")) {
            return Types.DATE;
        } else if(type == runtime.newSymbol("binary")) {
            return Types.BLOB;
        } else if(type == runtime.newSymbol("boolean")) {
            return Types.BOOLEAN;
        } else {
            return -1;
        }
    }

    private final static DateFormat FORMAT = new SimpleDateFormat("%y-%M-%d %H:%m:%s");

    private static void setValue(PreparedStatement ps, int index, Ruby runtime, ThreadContext context,
            IRubyObject value, IRubyObject type) throws SQLException {
        final int tp = getTypeValueFor(runtime, type);
        if(value.isNil()) {
            ps.setNull(index, tp);
            return;
        }

        switch(tp) {
        case Types.VARCHAR:
        case Types.CLOB:
            ps.setString(index, RubyString.objAsString(context, value).toString());
            break;
        case Types.INTEGER:
            ps.setLong(index, RubyNumeric.fix2long(value));
            break;
        case Types.FLOAT:
            ps.setDouble(index, ((RubyNumeric)value).getDoubleValue());
            break;
        case Types.TIMESTAMP:
        case Types.TIME:
        case Types.DATE:
            if(!(value instanceof RubyTime)) {
                try {
                    Date dd = FORMAT.parse(RubyString.objAsString(context, value).toString());
                    ps.setTimestamp(index, new java.sql.Timestamp(dd.getTime()), Calendar.getInstance());
                } catch(Exception e) {
                    ps.setString(index, RubyString.objAsString(context, value).toString());
                }
            } else {
                RubyTime rubyTime = (RubyTime) value;
                java.util.Date date = rubyTime.getJavaDate();
                long millis = date.getTime();
                long micros = rubyTime.microseconds() - millis / 1000;
                java.sql.Timestamp ts = new java.sql.Timestamp(millis);
                java.util.Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                ts.setNanos((int)(micros * 1000));
                ps.setTimestamp(index, ts, cal);
            }
            break;
        case Types.BOOLEAN:
            ps.setBoolean(index, value.isTrue());
            break;
        default: throw new RuntimeException("type " + type + " not supported in _bind yet");
        }
    }

    private static void setValuesOnPS(PreparedStatement ps, Ruby runtime, ThreadContext context,
            IRubyObject values, IRubyObject types) throws SQLException {
        RubyArray vals = (RubyArray)values;
        RubyArray tps = (RubyArray)types;

        for(int i=0, j=vals.getLength(); i<j; i++) {
            setValue(ps, i+1, runtime, context, vals.eltInternal(i), tps.eltInternal(i));
        }
    }

    /*
     * sql, values, types, name = nil, pk = nil, id_value = nil, sequence_name = nil
     */
    @JRubyMethod(name = "insert_bind", required = 3, rest = true)
    public static IRubyObject insert_bind(final ThreadContext context, IRubyObject recv, final IRubyObject[] args) throws SQLException {
        final Ruby runtime = recv.getRuntime();
        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                PreparedStatement ps = null;
                try {
                    ps = c.prepareStatement(rubyApi.convertToRubyString(args[0]).toString(), Statement.RETURN_GENERATED_KEYS);
                    setValuesOnPS(ps, runtime, context, args[1], args[2]);
                    ps.executeUpdate();
                    return unmarshal_id_result(runtime, ps.getGeneratedKeys());
                } finally {
                    try {
                        ps.close();
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    /*
     * sql, values, types, name = nil
     */
    @JRubyMethod(name = "update_bind", required = 3, rest = true)
    public static IRubyObject update_bind(final ThreadContext context, IRubyObject recv, final IRubyObject[] args) throws SQLException {
        final Ruby runtime = recv.getRuntime();
        Arity.checkArgumentCount(runtime, args, 3, 4);
        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                PreparedStatement ps = null;
                try {
                    ps = c.prepareStatement(rubyApi.convertToRubyString(args[0]).toString());
                    setValuesOnPS(ps, runtime, context, args[1], args[2]);
                    ps.executeUpdate();
                } finally {
                    try {
                        ps.close();
                    } catch (Exception e) {
                    }
                }
                return runtime.getNil();
            }
        });
    }

    /*
     * (is binary?, colname, tablename, primary key, id, value)
     */
    @JRubyMethod(name = "write_large_object", required = 6)
    public static IRubyObject write_large_object(IRubyObject recv, final IRubyObject[] args)
            throws SQLException, IOException {
        final Ruby runtime = recv.getRuntime();
        return withConnectionAndRetry(recv, new SQLBlock() {
            public IRubyObject call(Connection c) throws SQLException {
                String sql = "UPDATE " + rubyApi.convertToRubyString(args[2])
                        + " SET " + rubyApi.convertToRubyString(args[1])
                        + " = ? WHERE " + rubyApi.convertToRubyString(args[3])
                        + "=" + rubyApi.convertToRubyString(args[4]);
                PreparedStatement ps = null;
                try {
                    ps = c.prepareStatement(sql);
                    if (args[0].isTrue()) { // binary
                        ByteList outp = rubyApi.convertToRubyString(args[5]).getByteList();
                        ps.setBinaryStream(1, new ByteArrayInputStream(outp.bytes,
                                outp.begin, outp.realSize), outp.realSize);
                    } else { // clob
                        String ss = rubyApi.convertToRubyString(args[5]).getUnicodeValue();
                        ps.setCharacterStream(1, new StringReader(ss), ss.length());
                    }
                    ps.executeUpdate();
                } finally {
                    try {
                        ps.close();
                    } catch (Exception e) {
                    }
                }
                return runtime.getNil();
            }
        });
    }

    private static Connection getConnection(IRubyObject recv) {
        return getConnection(recv, false);
    }

    private static Connection getConnection(IRubyObject recv, boolean error) {
        Connection conn = (Connection) recv.dataGetStruct();
        if(error && conn == null) {
            RubyClass err = recv.getRuntime().getModule("ActiveRecord").getClass("ConnectionNotEstablished");
            throw new RaiseException(recv.getRuntime(), err, "no connection available", false);
        }
        return conn;
    }

    private static RuntimeException wrap(IRubyObject recv, Throwable exception) {
        RubyClass err = recv.getRuntime().getModule("ActiveRecord").getClass("ActiveRecordError");
        return (RuntimeException) new RaiseException(recv.getRuntime(), err, exception.getMessage(), false).initCause(exception);
    }

    private static ResultSet intoResultSet(IRubyObject inp) {
        JavaObject jo;
        if (inp instanceof JavaObject) {
            jo = (JavaObject) inp;
        } else {
            jo = (JavaObject) rubyApi.getInstanceVariable(inp, "@java_object");
        }
        return (ResultSet) jo.getValue();
    }

    private static boolean isConnectionBroken(IRubyObject recv, Connection c) {
        try {
            IRubyObject alive = config_value(recv, "connection_alive_sql");
            if (select_p(recv, alive).isTrue()) {
                String connectionSQL = rubyApi.convertToRubyString(alive).toString();
                Statement s = c.createStatement();
                try {
                    s.execute(connectionSQL);
                } finally {
                    try { s.close(); } catch (SQLException ignored) {}
                }
                return false;
            } else {
                return !c.isClosed();
            }
        } catch (SQLException sx) {
            return true;
        }
    }

    private static IRubyObject setConnection(IRubyObject recv, Connection c) {
        Connection prev = getConnection(recv);
        if (prev != null) {
            try {
                prev.close();
            } catch(Exception e) {}
        }
        IRubyObject rubyconn = recv.getRuntime().getNil();
        if (c != null) {
            rubyconn = wrappedConnection(recv,c);
        }
        rubyApi.setInstanceVariable(recv, "@connection", rubyconn);
        recv.dataWrapStruct(c);
        return recv;
    }

    private static IRubyObject wrappedConnection(IRubyObject recv, Connection c) {
        return Java.java_to_ruby(recv, JavaObject.wrap(recv.getRuntime(), c), Block.NULL_BLOCK);
    }

    private static JdbcConnectionFactory getConnectionFactory(IRubyObject recv) throws RaiseException {
        IRubyObject connection_factory = rubyApi.getInstanceVariable(recv, "@connection_factory");
        JdbcConnectionFactory factory = null;
        try {
            factory = (JdbcConnectionFactory) JavaEmbedUtils.rubyToJava(
                    recv.getRuntime(), connection_factory, JdbcConnectionFactory.class);
        } catch (Exception e) {
            factory = null;
        }
        if (factory == null) {
            throw recv.getRuntime().newRuntimeError("@connection_factory not set properly");
        }
        return factory;
    }

    private static IRubyObject config_value(IRubyObject recv, String key) {
        Ruby runtime = recv.getRuntime();
        IRubyObject config_hash = rubyApi.getInstanceVariable(recv, "@config");
        return rubyApi.callMethod(config_hash, "[]", runtime.newSymbol(key));
    }
}
