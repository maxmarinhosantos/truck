import java.util.logging.Level
import java.util.logging.Logger

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
/* Name: Execute Flow Node                                                         */
/*                                                                    */
/* Description: Execute a list of flownodes                */
/*                                                                    */
/* ****************************************************************** */

	Logger logger = Logger.getLogger("org.bonitasoft.groovymaintenance.executeflownode");

	StringBuffer analysis = new StringBuffer();

	// flow nodes
	// Attention, at this moment, the list may be a list of whatever - soon more control
	List<Object> listFlowNodesId= {{listNodeIdList;tips:Please give a list of nodes id, separated by a comma;type:LIST;default:124, 323}};

	int tenantId = 1;
 
	
	final PlatformServiceAccessor platformServiceAccessor = ServiceAccessorFactory.getInstance().createPlatformServiceAccessor();
	final TransactionService transactionService = platformServiceAccessor.getTransactionService();
	final TenantServiceAccessor tenantServiceAccessor = platformServiceAccessor.getTenantServiceAccessor(tenantId);
 
	analysis.append("Support Execute Flownodes: " + listFlowNodesId);
	try {
		for (Object flowNodeSt : listFlowNodesId) {	
			analysis.append("flowNodeSt=["+flowNodeSt+"]");
			logger.info("flowNode= : ["+flowNodeSt+"]");
			Long iteratorLong = Long.parseLong( flowNodeSt.toString() );
			transactionService.executeInTransaction(new ExecuteFlowNodes(tenantServiceAccessor, iteratorLong));
		}
 
		analysis.append(" -- SUCCESS");
	} catch (Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		analysis.append("Exception="+ sw.toString());
		return analysis.toString()
	}
 
	logger.log(Level.INFO, analysis.toString());
	return "success :"+analysis.toString();
 