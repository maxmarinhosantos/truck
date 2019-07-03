
import org.bonitasoft.engine.api.ProcessAPI ;

Long processInstance = {{processInstanceId;tips:This process instance will be cancelled (archived)}}
ProcessAPI processAPI = apiAccessor.getProcessAPI();
try
{
   processAPI.cancelProcessInstance( processInstance ) ;
   return "Case "+processInstance+" was successfully cancelled (archived)";
}
catch(Exception e)
{
    return e.getMessage();
}
