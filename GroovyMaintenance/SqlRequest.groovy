import java.util.logging.Level
import java.util.logging.Logger



/* ******************************************************************* */
/*                                                                     */
/* Name: Executing a SQL Request                                       */
/*                                                                     */
/* Description: The SQL Request is executed on the Engine Database     */
/*                                                                     */
/* ******************************************************************* */

Logger logger = Logger.getLogger("org.bonitasoft.groovymaintenance.executeflownode");

StringBuilder analysis = new StringBuilder();

// flow nodes
// Attention, at this moment, the list may be a list of whatever - soon more control
String sqlRequest= {{sqlRequestBonita;type:TEXT;default:select ID, STARTDATE ,NAME from process_instance}};
int maxRecords = {{MaxRecords;type:INTEGER;default:1000}};



analysis.append("SqlRequest ["+sqlRequest+"]...<br>");
List<Map<String,Object>> listResult = towTruckToolbox.executeSqlRequest( sqlRequest, maxRecords );

analysis.append("<br>");
analysis.append( towTruckToolbox.decoratorHtmlFormated(listResult));


return analysis.toString();


