package com.bonitasoft.custompage.towtruck.groovymaintenance;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.bonitasoft.log.event.BEvent;

import com.bonitasoft.custompage.towtruck.groovymaintenance.AttributeHolder.TypeSqlResult;

public class EngineSqlRequest {

    private static final BEvent EVENT_NO_DATASOURCE_FOUND = new BEvent(EngineSqlRequest.class.getName(), 1L, BEvent.Level.APPLICATIONERROR, "No Datasource detected",
            "No datasource for Bonita Engine is found",
            "Sql Request can't be executed.",
            "Check the list of Datasource");
    private static final BEvent EVENT_SQLEXECUTION_ERROR = new BEvent(EngineSqlRequest.class.getName(), 2L, BEvent.Level.APPLICATIONERROR, "Error SQL",
            "An SQL executin failed",
            "SQL Request can't be executed.",
            "Check the SQL request");
    public static Logger logger = Logger.getLogger(EngineSqlRequest.class.getName());

    public int totalCountSqlQuery;
    public int selectTop = -1;
    public TypeSqlResult colSqlResult = TypeSqlResult.UPPERCASE;

    public class SqlResult {
        public List<Map<String, Object>> listRecordsSqlQuery = new ArrayList<>();
        public List<BEvent> listEventSqlQuery = new ArrayList<>();

    }
    /**
     * @param sqlRequest
     */
    public SqlResult executeSqlQuery(String sqlRequest, List<Object> parameters) {
        logger.info("Custompage_twoTruck.AttributHolder execute sqlRequest[" + sqlRequest + "]");
        SqlResult sqlResult = new SqlResult();
        
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            DataSource dataSource = getDataSourceConnection();
            if (dataSource == null) {
                sqlResult.listEventSqlQuery.add(EVENT_NO_DATASOURCE_FOUND);
                return sqlResult;
            }
            con = dataSource.getConnection();

            pstmt = con.prepareStatement(sqlRequest);
            
            if (parameters!=null)
                for (int i=0;i<parameters.size();i++)
                {
                    pstmt.setObject(i+1, parameters.get( i ));
                }
            rs = pstmt.executeQuery();
            this.totalCountSqlQuery = 0;
            while (rs.next()) {
                this.totalCountSqlQuery += 1;
                if (this.selectTop== -1 || this.totalCountSqlQuery <= this.selectTop) {
                    Map<String, Object> record = new HashMap<>();

                    ResultSetMetaData rsMetaData = rs.getMetaData();
                    for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                        String colName = rsMetaData.getColumnName(i);
                        record.put(this.colSqlResult == TypeSqlResult.UPPERCASE ? colName.toUpperCase() : colName.toLowerCase(), rs.getObject(i));
                    }
                    sqlResult.listRecordsSqlQuery.add(record);
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("CustomPageTowTruck.AttributHolder.executeSqlQuery Error during execute Sql[" + sqlRequest + "] : " + e.toString() +
                    " : " + exceptionDetails);
            sqlResult.listEventSqlQuery.add(new BEvent(EVENT_SQLEXECUTION_ERROR, e, "SqlRequest=[" + sqlRequest + "]"));
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (SQLException localSQLException3) {
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                    pstmt = null;
                } catch (SQLException localSQLException4) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                    con = null;
                } catch (SQLException localSQLException5) {
                }
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (SQLException localSQLException6) {
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                    pstmt = null;
                } catch (SQLException localSQLException7) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                    con = null;
                } catch (SQLException localSQLException8) {
                }
            }
        }
        return sqlResult;
    }

    public Connection getConnection() throws SQLException {
        DataSource dataSource = getDataSourceConnection();
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        return null;
    }

    private String[] listDataSourcesEngine = { "java:/comp/env/bonitaSequenceManagerDS",
            "java:jboss/datasources/bonitaSequenceManagerDS" };

    private DataSource getDataSourceConnection() {
        String msg = "";
        List<String> listDatasourceToCheck = new ArrayList<String>();
        for (String dataSourceString : this.listDataSourcesEngine) {
            listDatasourceToCheck.add(dataSourceString);
        }
        for (String dataSourceString : listDatasourceToCheck) {
            try {
                Object ctx = new InitialContext();
                return (DataSource) ((Context) ctx).lookup(dataSourceString);
            } catch (NamingException e) {
                msg = msg + "DataSource[" + dataSourceString + "] : error " + e.toString() + ";";
            }
        }
        logger.severe("CustomPageTowTruck.AttributHolder.getDataSourceConnection: Can't found a datasource : " + msg);
        return null;
    }
}
