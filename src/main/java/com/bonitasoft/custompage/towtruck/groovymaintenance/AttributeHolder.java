package com.bonitasoft.custompage.towtruck.groovymaintenance;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bonitasoft.log.event.BEvent;

public class AttributeHolder
{
  private static final BEvent EVENT_BAD_DECODAGE = new BEvent(AttributeHolder.class.getName(), 3L, BEvent.Level.APPLICATIONERROR, "Bad decodage", 
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
  public String placeholder;
  public TypeAttribute type;
  public String databaseProductName;
  
  EngineSqlRequest engineSqlRequest = new EngineSqlRequest();
  public static enum TypeAttribute
  {
    STRING,  TEXT,  INTEGER,  HIDDEN,  READONLY,  SQL,  JSON,  LIST;
  }
  
  public Map<String, String> mapSqlRequests = new HashMap<String, String> ();
  
  public static enum TypeSqlResult
  {
    UPPERCASE,  LOWERCASE;
  }
  

  
  public AttributeHolder(String placeHolderString)
  {
    this.key = placeHolderString;
  }
  
  public static AttributeHolder getInstanceFromSerialisation(String code)
  {
    AttributeHolder attributeHolder = new AttributeHolder(code);
    attributeHolder.decodeAttribute();
    return attributeHolder;
  }
  
  public String serialize()
  {
    return this.key;
  }
  
  public List<BEvent> decodeAttribute()
  {
      List<BEvent> listEvents = new ArrayList<BEvent>();
    String attribute = "";
    try
    {
      String decodeKey = this.key.replace("\n", "");
      String[] listAttribute = decodeKey.split(";");
      this.name = listAttribute[0];
      this.label = this.name;
      this.type = TypeAttribute.STRING;
      for (int i = 1; i < listAttribute.length; i++)
      {
        attribute = listAttribute[i].trim();
        String[] attributeSplit = attribute.split(":");
        if (attribute.startsWith("tips:")) {
          this.tips = attributeSplit[1];
        }
        if (attribute.startsWith("type:")) {
          this.type = TypeAttribute.valueOf(attributeSplit[1].toUpperCase());
        }
        if (attribute.startsWith("colnameresult:")) {
            engineSqlRequest.colSqlResult = TypeSqlResult.valueOf(attributeSplit[1].toUpperCase());
        }
        if (attribute.startsWith("selecttop:")) {
            engineSqlRequest.selectTop = Integer.parseInt(attributeSplit[1]);
        }
        if (attribute.startsWith("label:")) {
          this.label = attributeSplit[1];
        }
        if (attribute.startsWith("placeholder:")) {
          this.placeholder = attributeSplit[1];
        }
        if (attribute.startsWith("default:"))
        {
          this.defaultValue = attributeSplit[1];
          this.value = this.defaultValue;
        }
        if (attribute.startsWith("sqlrequest:")) {
          this.mapSqlRequests.put(attributeSplit[1], attributeSplit[2]);
        }
      }
      if (TypeAttribute.SQL.equals(this.type))
      {
        
        try ( Connection con = engineSqlRequest.getConnection() ) {
            this.databaseProductName = con.getMetaData().getDatabaseProductName();
        }
        catch (Exception e)
        {
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          String exceptionDetails = sw.toString();
          logger.severe("CustomPageTowTruck.AttributHolder.executeSqlQuery Error during execute getDatabaseProperties: " + e.toString() + 
            " : " + exceptionDetails);
          return listEvents;
        }
        
      }
      return listEvents;
    }
    catch (Exception e)
    {
      listEvents.add(new BEvent(EVENT_BAD_DECODAGE, e, "Attribut name=[" + this.name + "] Attribut[" + attribute + "]"));
    }
    return listEvents;
  }
  
  public List<BEvent> execute(Map<String, AttributeHolder> allAnotherAttributes)
  {
    List<BEvent> listEvents = new ArrayList<>();
    if (TypeAttribute.SQL.equals(this.type))
    {
      String sqlRequest = this.mapSqlRequests.get(this.databaseProductName);
      if (sqlRequest == null) {
        sqlRequest = (String)this.mapSqlRequests.get("all");
      }
      Map<String, Object> mapSql = new HashMap<>();
      mapSql.put("systemcurrenttimemillis", Long.valueOf(System.currentTimeMillis()));
      for (AttributeHolder attribut : allAnotherAttributes.values()) {
        mapSql.put(attribut.name, attribut.getShortValue());
      }
      PlaceHolder placeHolder = new PlaceHolder();
      String sqlRequestToExecute = placeHolder.replacePlaceHolder(sqlRequest, mapSql, "@@", "@@");
      engineSqlRequest.executeSqlQuery(sqlRequestToExecute, null);
      listEvents.addAll(this.listEventSqlQuery);
    }
    return listEvents;
  }
  
  public boolean isForm()
  {
    return !TypeAttribute.HIDDEN.equals(this.type);
  }
  
  public Map<String, Object> getForm()
  {
    Map<String, Object> result = new HashMap<>();
    result.put("label", this.label);
    result.put("name", this.name);
    result.put("type", this.type.toString());
    result.put("placeholder", this.placeholder);
    result.put("tips", this.tips);
    if (TypeAttribute.SQL.equals(this.type))
    {
      result.put("databasename", this.databaseProductName);
      result.put("value", null);
      if (this.listRecordsSqlQuery != null) {
        result.put("value", "SqlCount(" + this.listRecordsSqlQuery.size() + ") " + (this.totalCountSqlQuery > this.listRecordsSqlQuery.size() ? "/ total " + this.totalCountSqlQuery : ""));
      }
    }
    else
    {
      result.put("value", this.value);
    }
    return result;
  }
  
  public List<BEvent> setHumanValue(String value)
  {
    List<BEvent> listEvents = new ArrayList<BEvent>();
    this.value = value;
    
    return listEvents;
  }
  
  public String getShortValue()
  {
    return this.value;
  }
  
  public String getValue()
  {
    if ((TypeAttribute.STRING.equals(this.type)) || 
      (TypeAttribute.HIDDEN.equals(this.type)) || 
      (TypeAttribute.READONLY.equals(this.type)) || 
      (TypeAttribute.TEXT.equals(this.type))) {
      return "\"" + this.value + "\"";
    }
    if (TypeAttribute.INTEGER.equals(this.type)) {
      return this.value;
    }
    if (TypeAttribute.JSON.equals(this.type))
    {
      // visit https://www.rgagnon.com/javadetails/java-0476.html
        String valueFormated= value;
        int pos=0;
        int fromIndex=0;
      while (pos!=-1) {
          pos=valueFormated.indexOf("\"",fromIndex);
          if (pos != -1)
          {
              valueFormated=valueFormated.substring(0,pos)+"\\"+valueFormated.substring(pos);
              fromIndex = pos+2;
          }
      }
      return "org.json.simple.JSONValue.parse(\"" + valueFormated + "\")";
    }
    if (TypeAttribute.LIST.equals(this.type))
    {
      return "Arrays.asList(" + (this.value==null? "" : this.value) + ")";
    }
    if (TypeAttribute.SQL.equals(this.type))
    {
      String buildGroovyInitialisation = "{\n  def list=[]\n  Map record\n";
      for (Map<String, Object> record : this.listRecordsSqlQuery)
      {
        StringBuilder valueRecord = new StringBuilder();
        for (Entry<String, Object> entry : record.entrySet())
        {
          if (valueRecord.length() > 0) {
            valueRecord.append( ", ");
          }
          valueRecord.append( "\"" + entry.getKey() + "\": ");
          if (entry.getValue() == null) {
              valueRecord.append( "null");
          } else if (((entry.getValue() instanceof Long)) || ((entry.getValue() instanceof Integer))) {
              valueRecord.append( entry.getValue());
          } else {
              valueRecord.append( "\"" + entry.getValue().toString() + "\"");
          }
        }
        buildGroovyInitialisation = buildGroovyInitialisation + "  record=[" + valueRecord.toString() + "]\n  list.add(record)\n";
      }
      buildGroovyInitialisation = buildGroovyInitialisation + "  return list}()\n";
      return buildGroovyInitialisation;
    }
    return "\"\"";
  }
  
  public List<BEvent> listEventSqlQuery = new ArrayList<BEvent>();
  public List<Map<String, Object>> listRecordsSqlQuery = new ArrayList<Map<String, Object>>();
  public int totalCountSqlQuery;
  
  
}
