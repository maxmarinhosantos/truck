import org.bonitasoft.engine.api.ProcessAPI;

StringBuffer result= new StringBuffer(" start ");
ProcessAPI processAPI = apiAccessor.getProcessAPI();
// ProcessAPI processAPI = apiClient.getProcessAPI();
// ProcessAPI processAPI = restAPIContext.getApiClient().getProcessAPI();
if (processAPI==null)
	result.append( " NoProcessAPI" );
else
{
	long instances = processAPI.getNumberOfProcessInstances();

	result.append( "get ProcessAPI: ");
       result.append( String.valueOf( instances ));
}
return result.toString();