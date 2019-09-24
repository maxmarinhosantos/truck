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
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;

/**
 * create the AttributeHolder from the original string
 * 
 * @author Firstname Lastname
 */
public class AttributeHolder {

  private final static BEvent EVENT_NO_DATASOURCE_FOUND = new BEvent(AttributeHolder.class.getName(), 1, Level.APPLICATIONERROR, "No Datasource detected",
      "No datasource for Bonita Engine is found",
      "Sql Request can't be executed.",
      "Check the list of Datasource");

  private final static BEvent EVENT_SQLEXECUTION_ERROR = new BEvent(AttributeHolder.class.getName(), 2, Level.APPLICATIONERROR, "Error SQL",
      "An SQL executin failed",
      "SQL Request can't be executed.",
      "Check the SQL request");
  private final static BEvent EVENT_BAD_DECODAGE = new BEvent(AttributeHolder.class.getName(), 3, Level.APPLICATIONERROR, "Bad decodage",
      "One attribut definition is not correct",
      "Attribut definition is corrupted",
      "Check the definition");

  public static Logger logger = Logger.getLogger(AttributeHolder.class.getName());

  public String key;

  public String name;
  public String label;
  public String tips;
  public String defaultValue;
  public String value;

  /** format the input: may be json */
  public enum TypeAttribute {
    STRING, TEXT, INTEGER, HIDDEN, READONLY, SQL, JSON
  }

  public TypeAttribute type;

  /**
   * for the SQL fields
   */
  public String databaseProductName;
  public Map<String, String> mapSqlRequests = new HashMap<String, String>();

  public enum TypeSqlResult {
    UPPERCASE, LOWERCASE
  }

  public TypeSqlResult colSqlResult = TypeSqlResult.UPPERCASE;
  public int selectTop = -1;

  // {{ListFlowNodes;tips:Give a list of FlowNodes;type:json}}
  //    sqlrequest:postgresql:SELECT trigger_name FROM QRTZ_TRIGGERS WHERE ( NEXT_FIRE_TIME < extract(epoch from ( NOW() - interval '1 minute' )) OR START_TIME <> NEXT_FIRE_TIME ) AND TRIGGER_STATE = 'WAITING' AND TRIGGER_TYPE = 'SIMPLE'; 

  /*
   * First parameter : name (label too by default)
   * Additionnal parameters:
   * tips:<value> tips visible in the HTML page
   * type:[STRING,INTEGER,HIDDEN,READONLY,SQL,JSON] describe the type.
   * sqlrequest:<driver>:<sqlrequest> : driver are all (for all driver), postgres, h2, oracle,
   * mysql.
   * sqlrequest can contains place holder via @@placeholder@@ and @@systemcurrenttimemillis@@ is the
   * current time in millisecond
   * colnameresult: [UPPERCASE|LOWERCASE] the result is force in Upper or Lower case. Upper is the
   * default.
   * selecttop:<Number> if there are too much record, the number of record managed is this value.
   * label:<label>
   * default:<value> the default value
   */
  public AttributeHolder(String placeHolderString) {
    key = placeHolderString;
  }

  /**
   * serialize / deserialize the object
   * 
   * @param code
   * @return
   */
  public static AttributeHolder getInstanceFromSerialisation(String code) {
    AttributeHolder attributeHolder = new AttributeHolder(code);
    attributeHolder.decodeAttribute();
    return attributeHolder;

  }

  public String serialize() {
    return key;
  }

  public List<BEvent> decodeAttribute() {

    List<BEvent> listEvents = new ArrayList<BEvent>();
    String attribute = "";
    try {
      String decodeKey = key.replace("\n", ""); // allow the break line in the key
      String[] listAttribute = decodeKey.split(";");
      name = listAttribute[0];
      label = name;
      type = TypeAttribute.STRING;
      for (int i = 1; i < listAttribute.length; i++) {
        // decode
        attribute = listAttribute[i].trim();
        String[] attributeSplit = attribute.split(":");
        if (attribute.startsWith("tips:"))
          tips = attributeSplit[1];
        if (attribute.startsWith("type:"))
          type = TypeAttribute.valueOf(attributeSplit[1].toUpperCase());
        if (attribute.startsWith("colnameresult:"))
          colSqlResult = TypeSqlResult.valueOf(attributeSplit[1].toUpperCase());
        if (attribute.startsWith("selecttop:"))
          selectTop = Integer.valueOf(attributeSplit[1]);
        if (attribute.startsWith("label:"))
          label = attributeSplit[1];
        if (attribute.startsWith("default:")) {
          defaultValue = attributeSplit[1];
          value = defaultValue;
        }
        if (attribute.startsWith("sqlrequest:")) {
          mapSqlRequests.put(attributeSplit[1], attributeSplit[2]);
        }
      }

      if (TypeAttribute.SQL.equals(type)) {
        // search the product name
        Connection con = null;
        try {
          final DataSource dataSource = getDataSourceConnection();
          if (dataSource != null) {
            con = dataSource.getConnection();
            databaseProductName = con.getMetaData().getDatabaseProductName();
          }
        } catch (Exception e) {
          final StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          final String exceptionDetails = sw.toString();
          logger.severe("CustomPageTowTruck.AttributHolder.executeSqlQuery Error during execute getDatabaseProperties: " + e.toString()
              + " : " + exceptionDetails);

        } finally {
          if (con != null) {
            try {
              con.close();
              con = null;
            } catch (final SQLException localSQLException1) {
            }
          }
        }
      }
    } catch (Exception e) {
      listEvents.add(new BEvent(EVENT_BAD_DECODAGE, e, "Attribut name=[" + name + "] Attribut[" + attribute + "]"));
    }
    return listEvents;
  }

  /**
   * if the attribut is a Sql execution, then play it
   * 
   * @return
   */
  public List<BEvent> execute(Map<String, AttributeHolder> allAnotherAttributes) {
    List<BEvent> listEvents = new ArrayList<BEvent>();
    // execute the SqlRequest if we have to
    if (TypeAttribute.SQL.equals(type)) {
      String sqlRequest = mapSqlRequests.get(databaseProductName);
      if (sqlRequest == null)
        sqlRequest = mapSqlRequests.get("all");

      Map<String, Object> mapSql = new HashMap<String, Object>();
      mapSql.put("systemcurrenttimemillis", System.currentTimeMillis());
      for (AttributeHolder attribut : allAnotherAttributes.values())
        mapSql.put(attribut.name, attribut.getShortValue());

      PlaceHolder placeHolder = new PlaceHolder();
      String sqlRequestToExecute = placeHolder.replacePlaceHolder(sqlRequest, mapSql, "@@", "@@");
      executeSqlQuery(sqlRequestToExecute);
      listEvents.addAll(listEventSqlQuery);

    }

    return listEvents;
  }

  /**
   * the attribute may be hide for the form
   * 
   * @return
   */
  public boolean isForm() {
    return !(TypeAttribute.HIDDEN.equals(type));
  }

  /**
   * return information for the form
   * 
   * @return
   */
  public Map<String, Object> getForm() {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("label", label);
    result.put("name", name);
    result.put("type", type.toString());
    if (TypeAttribute.SQL.equals(type)) {
      result.put("databasename", databaseProductName);
      result.put("value", null);
      if (listRecordsSqlQuery != null)
        result.put("value", "SqlCount(" + listRecordsSqlQuery.size() + ") " + (totalCountSqlQuery > listRecordsSqlQuery.size() ? "/ total " + totalCountSqlQuery : ""));
    } else
      result.put("value", value);
    return result;
  }

  /* -------------------------------------------------------------------- */
  /*                                                                      */
  /* value calculation */
  /*                                                                      */
  /* -------------------------------------------------------------------- */

  public List<BEvent> setHumanValue(String value) {
    List<BEvent> listEvents = new ArrayList<BEvent>();
    this.value = value;
    // execute the SqlRequest if we have to
    return listEvents;
  }

  /**
   * if the attributes is used in an another parameters, we want a short value
   * Usecase : a SQL request include an another parameters
   * 
   * @return
   */
  public String getShortValue() {
    return value;
  }

  public String getValue() {

    if (TypeAttribute.STRING.equals(type)
        || TypeAttribute.HIDDEN.equals(type)
        || TypeAttribute.READONLY.equals(type)
        || TypeAttribute.JSON.equals(type)
        || TypeAttribute.TEXT.equals(type)) {
      return "\"" + value + "\"";
    } else if (TypeAttribute.INTEGER.equals(type)) {
      return value;
    } else if (TypeAttribute.SQL.equals(type)) {
      // return the SQL data for a Groovy point of vue
      /**
       * build
       * List<Map<String,Object>> listProcess= {
       * def list = []
       * Map recordtemp = ["displayName": "catch", "id": 4713106376816622092];
       * list.add(recordtemp);
       * return list;}()
       */
      String buildGroovyInitialisation = "{\n  def list=[]\n  Map record\n";
      for (Map<String, Object> record : listRecordsSqlQuery) {

        String valueRecord = "";
        for (String colId : record.keySet()) {
          if (valueRecord.length() > 0)
            valueRecord += ", ";

          valueRecord += "\"" + colId + "\": ";
          Object valueKey = record.get(colId);
          if (valueKey == null)
            valueRecord += "null";
          else if (valueKey instanceof Long || valueKey instanceof Integer)
            valueRecord += valueKey;
          else
            valueRecord += "\"" + valueKey.toString() + "\"";
        }
        buildGroovyInitialisation += "  record=[" + valueRecord + "]\n  list.add(record)\n";
      }
      buildGroovyInitialisation += "  return list}()\n";
      return buildGroovyInitialisation;
    }
    return "\"\"";
  }

  /**
   * execute a request, then populate the
   * 
   * @param sqlRequest
   */

  public List<BEvent> listEventSqlQuery = new ArrayList<BEvent>();
  public List<Map<String, Object>> listRecordsSqlQuery = new ArrayList<Map<String, Object>>();
  public int totalCountSqlQuery;

  private void executeSqlQuery(String sqlRequest) {
    logger.info("Custompage_twoTruck.AttributHolder execute sqlRequest[" + sqlRequest + "]");
    listRecordsSqlQuery = new ArrayList<Map<String, Object>>();
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      final DataSource dataSource = getDataSourceConnection();
      if (dataSource == null) {
        listEventSqlQuery.add(EVENT_NO_DATASOURCE_FOUND);
        return;
      }
      con = dataSource.getConnection();

      pstmt = con.prepareStatement(sqlRequest);

      rs = pstmt.executeQuery();
      totalCountSqlQuery = 0;
      while (rs.next()) {
        totalCountSqlQuery++;
        // we collect only the selectTop item
        if (totalCountSqlQuery > selectTop)
          continue;

        Map<String, Object> record = new HashMap<String, Object>();

        ResultSetMetaData rsMetaData = rs.getMetaData();
        for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
          String colName = rsMetaData.getColumnName(i);
          record.put(colSqlResult == TypeSqlResult.UPPERCASE ? colName.toUpperCase() : colName.toLowerCase(), rs.getObject(i));
        }
        listRecordsSqlQuery.add(record);
      }

    } catch (final Exception e) {
      final StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      final String exceptionDetails = sw.toString();
      logger.severe("CustomPageTowTruck.AttributHolder.executeSqlQuery Error during execute Sql[" + sqlRequest + "] : " + e.toString()
          + " : " + exceptionDetails);
      listEventSqlQuery.add(new BEvent(EVENT_SQLEXECUTION_ERROR, e, "SqlRequest=[" + sqlRequest + "]"));
    } finally {
      if (rs != null) {
        try {
          rs.close();
          rs = null;
        } catch (final SQLException localSQLException) {
        }
      }
      if (pstmt != null) {
        try {
          pstmt.close();
          pstmt = null;
        } catch (final SQLException localSQLException) {
        }
      }
      if (con != null) {
        try {
          con.close();
          con = null;
        } catch (final SQLException localSQLException1) {
        }
      }
    }
    return;
  }

  /**
   * retrieve the Bonita Engine datasource
   */
  private String[] listDataSourcesEngine = new String[] { "java:/comp/env/bonitaSequenceManagerDS", // tomcat
      "java:jboss/datasources/bonitaSequenceManagerDS" }; // jboss 

  private DataSource getDataSourceConnection() {
    // logger.info(loggerLabel+".getDataSourceConnection() start");

    String msg = "";
    List<String> listDatasourceToCheck = new ArrayList<String>();
    for (String dataSourceString : listDataSourcesEngine)
      listDatasourceToCheck.add(dataSourceString);

    for (String dataSourceString : listDatasourceToCheck) {
      // logger.info(loggerLabel+".getDataSourceConnection() check["+dataSourceString+"]");
      try {
        final Context ctx = new InitialContext();
        final DataSource dataSource = (DataSource) ctx.lookup(dataSourceString);
        // logger.info(loggerLabel+".getDataSourceConnection() ["+dataSourceString+"] isOk");
        return dataSource;
      } catch (NamingException e) {
        // logger.info(loggerLabel+".getDataSourceConnection() error["+dataSourceString+"] : "+e.toString());
        msg += "DataSource[" + dataSourceString + "] : error " + e.toString() + ";";
      }
    }
    logger.severe("CustomPageTowTruck.AttributHolder.getDataSourceConnection: Can't found a datasource : " + msg);
    return null;
  }

}
