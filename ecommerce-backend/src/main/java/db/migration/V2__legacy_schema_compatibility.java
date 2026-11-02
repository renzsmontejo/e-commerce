package db.migration;

import java.sql.*;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Preserves legacy columns/data while permitting inserts through the current entities. */
public class V2__legacy_schema_compatibility extends BaseJavaMigration {
    @Override public Integer getChecksum() { return 2; }
    @Override public void migrate(Context context) throws Exception {
        Connection connection=context.getConnection();
        if(column(connection,"products","brand")) execute(connection,"ALTER TABLE products ALTER COLUMN brand SET DEFAULT ''");
        if(column(connection,"products","is_featured")) execute(connection,"ALTER TABLE products ALTER COLUMN is_featured SET DEFAULT 0");
        execute(connection,"ALTER TABLE products MODIFY COLUMN description TEXT NULL");
        if(column(connection,"users","password")) execute(connection,"ALTER TABLE users MODIFY COLUMN password VARCHAR(200) NULL");
        try(var columns=connection.getMetaData().getColumns(connection.getCatalog(),null,"users","id")) {
            if(columns.next() && !"YES".equals(columns.getString("IS_AUTOINCREMENT"))) {
                try(var statement=connection.createStatement()) {
                    int previous;
                    try(var result=statement.executeQuery("SELECT @@SESSION.FOREIGN_KEY_CHECKS")){result.next();previous=result.getInt(1);}
                    statement.execute("SET SESSION FOREIGN_KEY_CHECKS=0");
                    try{statement.execute("ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");}
                    finally{statement.execute("SET SESSION FOREIGN_KEY_CHECKS="+previous);}
                }
            }
        }
    }
    private boolean column(Connection connection,String table,String column) throws SQLException {
        try(var columns=connection.getMetaData().getColumns(connection.getCatalog(),null,table,column)){return columns.next();}
    }
    private void execute(Connection connection,String sql) throws SQLException {
        try(var statement=connection.createStatement()){statement.execute(sql);}
    }
}
