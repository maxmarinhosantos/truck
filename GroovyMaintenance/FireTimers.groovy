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
/* Name: FireTimer                                                    */
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
  sqlrequest:all:SELECT job_name FROM QRTZ_TRIGGERS WHERE ( NEXT_FIRE_TIME < @@systemcurrenttimemillis@@ - 60000 OR START_TIME <> NEXT_FIRE_TIME ) AND TRIGGER_STATE = 'WAITING' AND TRIGGER_TYPE = 'SIMPLE';
  colnameresult:uppercase;
  selecttop:50
}}

int tenantId = 1;
StringWriter strW = new StringWriter();
PrintWriter pw = new PrintWriter(strW);


final PlatformServiceAccessor platformServiceAccessor = ServiceAccessorFactory.getInstance().createPlatformServiceAccessor();
final TransactionService transactionService = platformServiceAccessor.getTransactionService();
final TenantServiceAccessor tenantServiceAccessor = platformServiceAccessor.getTenantServiceAccessor(tenantId);


try {
		//final Iterator<Long> iterator = listQuartzJobs.iterator();
		//transactionService.executeInTransaction(new ExecuteFlowNodes(tenantServiceAccessor, iterator));
		// while (iterator.hasNext()) {
		for (Map record : listQuartzJobs)
		{

				String triggerName = record.get("JOB_NAME");

				// format is according case 22385:
				// For every flownode id extracted from the list of triggers obtained with the query given by Poorav: 
				// (e.g. Trigger: job_name = 'Timer_Ev_111111' ==> flownode ID: 111111)
				if (triggerName.startsWith("Timer_Ev_" ))
				{
						triggerName = triggerName.substring("Timer_Ev_".length());
						Long flowNode = Long.valueOf( triggerName );

						transactionService.executeInTransaction(new ExecuteFlowNodes(tenantServiceAccessor, flowNode));
				}
		}
} catch (Exception e) {
pw.println(" ====> failure " + e.getMessage());
e.printStackTrace(pw);
}



pw.println(" ====> S U C C E S S, executed "+listQuartzJobs.size()+" flownodes");


return strW.toString();
