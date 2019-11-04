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

List<Long> flownodesIds = {{listNodeId;tips:Please give a list of nodes id, separated by a comma;type:JSON;placeholder:[123,4424,553]}}

int tenantId = 1;
 
 StringWriter strW = new StringWriter();
 PrintWriter pw = new PrintWriter(strW);
 
 final PlatformServiceAccessor platformServiceAccessor = ServiceAccessorFactory.getInstance().createPlatformServiceAccessor();
 final TransactionService transactionService = platformServiceAccessor.getTransactionService();
 final TenantServiceAccessor tenantServiceAccessor = platformServiceAccessor.getTenantServiceAccessor(tenantId);
 
 pw.print("Case 00023339 Bonitasoft Support Execute Flownodes: " + flownodesIds);
 try {
 final Iterator<Long> iterator = flownodesIds.iterator();
 transactionService.executeInTransaction(new ExecuteFlowNodes(tenantServiceAccessor, iterator));
 while (iterator.hasNext()) {
 transactionService.executeInTransaction(new ExecuteFlowNodes(tenantServiceAccessor, iterator));
 }
 
 pw.println(" -- SUCCESS");
 } catch (Exception e) {
pw.print(" -- FAILURE -- ");
e.printStackTrace(pw);
 }
 
 String out = strW.toString();
 pw.close();
 strW.close();
 Logger.getLogger("org.bonitasoft").log(Level.WARNING, out);