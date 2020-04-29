package com.bonitasoft.custompage.towtruck.groovymaintenance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TowTruckToolbox {

    public TowTruckToolbox() {
        // nothing specialo to do
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Bonita Engine SQL Request */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public List<Map<String, Object>> executeSqlRequest(String sqlRequest, int maxRecord) {
        return executeSqlRequest(sqlRequest, null, maxRecord);
    }

    public List<Map<String, Object>> executeSqlRequest(String sqlRequest, List<Object> parameters, int maxRecord) {
        EngineSqlRequest engineSqlRequest = new EngineSqlRequest();
        engineSqlRequest.selectTop = maxRecord;
        engineSqlRequest.executeSqlQuery(sqlRequest, parameters);
        return engineSqlRequest.listRecordsSqlQuery;
    }

    
    public String decoratorHtmlFormated(List<Map<String, Object>> listValues) {
        StringBuilder result = new StringBuilder();
        List<String> listHeaders = getHeader(listValues);
        if (listValues == null || listValues.isEmpty() || listHeaders == null)
            return "No records";

        // Explore the size for each header
        Map<String, Integer> sizePerHeader = new HashMap<>();
        for (String header : listHeaders) {
            sizePerHeader.put(header, header.length());
        }

        for (Map<String, Object> record : listValues) {
            for (String header : listHeaders) {
                Integer size = sizePerHeader.get(header);
                Object value = record.get(header);
                if (value != null && value.toString().length() > size.intValue())
                    sizePerHeader.put(header, value.toString().length());
            }
        }

        // produce the header
        result.append("<table class=\"table table-bordered table-condensed\">");
        
        result.append("<tr>");
        for (String header : listHeaders) {
            result.append("<th>"+header+ "</th>");
        }
        result.append("</tr>");


        // produce  the result

        for (Map<String, Object> record : listValues) {
            result.append("<tr>");
            for (String header : listHeaders) {
                Object value = record.get(header);
                result.append("<td>"+(value == null ? "" : value.toString()) + "</td>");
            }
            result.append("</tr>");
        }

        result.append("</table>");
        result.append("Number of records : "+listValues.size());
        return result.toString();
        
        
        
    }
    
    public String decoratorTextFormated(List<Map<String, Object>> listValues) {
        return decoratorFormated(listValues, " ", "\n");
    }
    /**
     * @param listValues
     * @return
     */

    private String decoratorFormated(List<Map<String, Object>> listValues, String blanck, String carriageReturn) {
        StringBuilder result = new StringBuilder();
        List<String> listHeaders = getHeader(listValues);
        if (listValues == null || listValues.isEmpty() || listHeaders == null)
            return "No records";

        // Explore the size for each header
        Map<String, Integer> sizePerHeader = new HashMap<>();
        for (String header : listHeaders) {
            sizePerHeader.put(header, header.length());
        }

        for (Map<String, Object> record : listValues) {
            for (String header : listHeaders) {
                Integer size = sizePerHeader.get(header);
                Object value = record.get(header);
                if (value != null && value.toString().length() > size.intValue())
                    sizePerHeader.put(header, value.toString().length());
            }
        }

        // produce the header
        result.append("|");
        for (String header : listHeaders) {
            result.append(completeString(header, sizePerHeader.get(header).intValue(), blanck) + "|");
        }
        result.append(carriageReturn+"|");
        for (String header : listHeaders) {
            result.append(completeString("-", sizePerHeader.get(header).intValue(), "-") + "|");
        }
        result.append(carriageReturn);

        // produce  the result

        for (Map<String, Object> record : listValues) {
            result.append("|");
            for (String header : listHeaders) {
                Object value = record.get(header);
                result.append(completeString(value == null ? "" : value.toString(), sizePerHeader.get(header).intValue(), blanck) + "|");
            }
            result.append(carriageReturn);
        }

        return result.toString();

    }

    public String decoratorCsv(List<Map<String, Object>> listValues) {
        StringBuilder result = new StringBuilder();
        List<String> listHeaders = getHeader(listValues);
        if (listValues == null || listValues.isEmpty() || listHeaders == null)
            return result.toString();

        // Explore the size for each header

        // produce  the result
        for (Map<String, Object> record : listValues) {
            for (int i = 0; i < listHeaders.size(); i++) {
                if (i > 0)
                    result.append(";");
                result.append(record.get(listHeaders.get(i)));
            }
            result.append("\n");
        }

        return result.toString();
    }

    private String completeString(String value, int size, String completion) {
        StringBuilder result = new StringBuilder();
        result.append(value);
        int missing = size-result.length();
        for (int i=0;i<missing;i++)
            result.append(completion);
        return result.toString();

    }

    private List<String> getHeader(List<Map<String, Object>> listValues) {
        if (listValues.isEmpty())
            return null;

        return listValues.get(0).keySet().stream().collect(Collectors.toList());
    }

}
