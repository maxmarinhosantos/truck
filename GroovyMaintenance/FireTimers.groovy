import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.execution.work.ExecuteFlowNodes;
import com.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.service.PlatformServiceAccessor;
import com.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.transaction.TransactionService;
import java.io.StringWriter;
import java.io.PrintWriter;

/* ****************************************************************** */
/*                                                                    */
/* Name: ReamTimer                                                    */
/*                                                                    */
/* Description: Detect all timer not fired, then fired them           */
/*                                                                    */
/* ****************************************************************** */

/**
* PARAMETERS: put your FLOWNODE ID list in 'flownodesIds' variable
**/

List<Long> flownodesIds = null; // { { ListFlowNodes;tips:Give a list of FlowNodes;type:json } };

List<String> listQuartzJobs = {{ListQuartz;
  type:sql;
  sqlrequest:all:SELECT trigger_name FROM QRTZ_TRIGGERS WHERE ( NEXT_FIRE_TIME < @@systemcurrenttimemillis@@ - 60000 OR START_TIME <> NEXT_FIRE_TIME ) AND TRIGGER_STATE = 'WAITING' AND TRIGGER_TYPE = 'SIMPLE';
  colnameresult:uppercase
}}


int tenantId = 1;



String pw="";


// explode first the l
try {
  if (flownodesIds==null || flownodesIds.size() == 0)
  {
    flownodesIds = new ArrayList<Long>();
    if (listQuartzJobs.size()>0 )
    {
      for (Map record : listQuartzJobs)
      {
        String triggerName = record.get("TRIGGER_NAME");

        // format is according case 22385:
        // For every flownode id extracted from the list of triggers obtained with the query given by Poorav: 
        // (e.g. Trigger: job_name = 'Timer_Ev_111111' ==> flownode ID: 111111)
        if (triggerName.startsWith("Timer_Ev_" ))
        {
          triggerName = triggerName.substring("Timer_Ev_".length());
          Long flowNode = Long.valueOf( triggerName );
          flownodesIds.add( flowNode );
        }
      }
      pw+= "FlowNode Detected from database : "+flownodesIds;
    }
  }
}
catch(Exception e)
{
  pw+=" Failure when explode the SqlArray"
}

ProcessAPI processAPI = apiAccessor.getProcessAPI();
try
{
  pw+="Execute:"
  for (Long flowNodeId : flownodesIds)
  {
    processAPI.executeFlowNode( flowNodeId);
    pw+=flowNodeId+",";
  }

  pw += " ====> S U C C E S S, executed "+flownodesIds.size()+" flownodes";

} catch (Exception e) {
  final StringWriter sw = new StringWriter();
  e.printStackTrace(new PrintWriter(sw));
  final String exceptionDetails = sw.toString();

  pw+=" ====> failure " + e.getMessage();
  pm+=exceptionDetails;
}

return pw;