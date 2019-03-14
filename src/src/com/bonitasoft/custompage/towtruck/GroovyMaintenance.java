package com.bonitasoft.custompage.towtruck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStore.DetectionParameters;
import org.bonitasoft.store.BonitaStore.UrlToDownload;
import org.bonitasoft.store.BonitaStoreAPI;
import org.bonitasoft.store.BonitaStoreGit;
import org.bonitasoft.store.StoreResult;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.artefact.Artefact.TypeArtefact;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.codehaus.groovy.control.CompilerConfiguration;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;


public class GroovyMaintenance {
  
  
  private final static BEvent NO_CODE_FOUND = new BEvent(GroovyMaintenance.class.getName(), 1, Level.APPLICATIONERROR, "This Groovy does code does not exists", 
      "No groovy is found with this code.",
      "Nothing to propose",
      "Check the code (and the content of the git repository)");
  private final static BEvent SUSPICIOUS_GROOVY = new BEvent(GroovyMaintenance.class.getName(), 2, Level.APPLICATIONERROR, "Suspicious groovy", 
      "The groovy loaded is suspicious",
      "The groovy may be not correct, and execution may be not expected",
      "Check the Groovy code");
  
  private final static BEvent NO_GROOVY_SOURCE = new BEvent(GroovyMaintenance.class.getName(), 3, Level.APPLICATIONERROR, "No Groovy Source", 
      "No groovy source is not found",
      "No groovy script is executed",
      "Check the session : you may be disconnected");
  
  private final static BEvent GROOVY_DOWNLOADED = BEvent.getInstanceShortSuccess(GroovyMaintenance.class.getName(), 4,"Groovy Downloaded" );

  private final static BEvent GROOVY_EXECUTED = BEvent.getInstanceShortSuccess(GroovyMaintenance.class.getName(), 5, "Groovy executed with success");
  
  
  private final static BEvent GROOVY_EXECUTION_FAILED = new BEvent(GroovyMaintenance.class.getName(), 6, Level.APPLICATIONERROR, "Groovy executioopn failed", 
      "The groovy send an error",
      "Script is not executed",
      "Check the result");
  /*
   * Load a groovy maintenance for Github
   */
  public static Map<String,Object> getGroovyMaintenance(HttpServletRequest request, String groovyCode)
  {
    Map<String,Object> result = new HashMap<String,Object> ();
    List<BEvent> listEvents = new ArrayList<BEvent>();
    
    LoggerStore logBox = new LoggerStore();
    
    BonitaStoreGit bonitaStore = BonitaStoreAPI.getGitStore( BonitaStoreAPI.CommunityGithubUserName, BonitaStoreAPI.CommunityGithubPassword, "https://api.github.com/repos/Bonitasoft-Community/page_towntruck");
    bonitaStore.setSpecificRepository("/contents/GroovyMaintenance");
    DetectionParameters detectionParameters = new BonitaStore.DetectionParameters();
    detectionParameters.listTypeArtefact = Arrays.asList(TypeArtefact.GROOVY);
    
    StoreResult storeResult = bonitaStore.getListArtefacts(detectionParameters, logBox);
    listEvents.addAll( storeResult.getEvents());
    Artefact groovyArtefact = storeResult.getArtefactByName(groovyCode+".groovy");  
    if (groovyArtefact == null)
    {
      listEvents.add( new BEvent( NO_CODE_FOUND, "Code["+groovyCode+"]"));
    }
    else
    {
      List<Object> listPlaceHolder = new ArrayList<Object>();
      // load it
      storeResult = bonitaStore.downloadArtefact(groovyArtefact, UrlToDownload.URLDOWNLOAD, logBox);
      
      listEvents.addAll( storeResult.getEvents());
      if (! BEventFactory.isError( listEvents) )
      {
        String groovySource = storeResult.content;
        // detect all place holder
        int scanPosition=0;
        while (scanPosition < groovySource.length())
        {
          int nextPlaceHolder = groovySource.indexOf("{{", scanPosition);
          if (nextPlaceHolder==-1)
            scanPosition = groovySource.length();
          else
          {
            scanPosition = nextPlaceHolder+2;
            int endPlaceHolder = groovySource.indexOf("}}", nextPlaceHolder+2);
            if (endPlaceHolder!=-1)
            {
              String placeHolder = groovySource.substring(nextPlaceHolder+2, endPlaceHolder);
              
              // place holder can be a simple key, or contains something like "{{sqllist;tips:Give a list of the different item}}
              Map<String,Object> onePlaceHolderMap = new HashMap<String,Object>();
              String[] listAttribute = placeHolder.split(";");
              onePlaceHolderMap.put("key", placeHolder);
              onePlaceHolderMap.put("label", listAttribute[ 0 ]);
              for( int i=1;i<listAttribute.length;i++)
              {
                String attribute = listAttribute[ i ];
                String[] attributeSplit = attribute.split(":");
                if (attributeSplit.length==2)
                  onePlaceHolderMap.put( attributeSplit[ 0], attributeSplit[ 1 ]);
                
              }
              listPlaceHolder.add( onePlaceHolderMap );
              scanPosition= endPlaceHolder+2;
            }
            else // end end place holder, then we can stop
            {
              scanPosition = groovySource.length();
              listEvents.add( new BEvent( SUSPICIOUS_GROOVY, "Place Holder starts {{ at position ["+nextPlaceHolder+"] and not finish"));
            }
            }
          } // end detection place holder
        if (! BEventFactory.isError( listEvents ))
          listEvents.add( GROOVY_DOWNLOADED);
        
        // we saved the groovy in the Tomcat session
        if (request!=null)
        {
          HttpSession httpSession = request.getSession();
          httpSession.setAttribute("groovy",groovySource);
        }
        result.put("placeholder", listPlaceHolder);
      }
    }
    result.put("listevents", BEventFactory.getHtml(listEvents));
  
    return result;    
  }

  /**
   * execute a Groovy Maintenance code
   * @param request
   * @param groovySource
   * @param placeHolder
   * @param binding
   * @return
   */
  public static Map<String,Object> executeGroovyMaintenance(HttpServletRequest request, String groovySource, List<Map<String,String>> listPlaceHolders, Binding binding )
  {
    
    Map<String,Object> result = new HashMap<String,Object> ();
    List<BEvent> listEvents = new ArrayList<BEvent>();
    if (groovySource==null && request!=null)
      {
        HttpSession httpSession = request.getSession();
        groovySource = (String) httpSession.getAttribute("groovy");
      }
    
    if (groovySource==null)
    {
      listEvents.add( NO_GROOVY_SOURCE);
    }
    else
    {
      if (listPlaceHolders!=null)
      {
        // replace all place holder now
        for (Map<String, String> placeHolder : listPlaceHolders)
        {
          int loop=0;
          // do not use the replaceAll : the place holder can be interpreted as a expression
          while (groovySource.contains("{{"+placeHolder.get("key")+"}}") && loop < 100)
          {
            String valueToReplace=placeHolder.get("value");
            if (valueToReplace==null)
              valueToReplace="";
           
            groovySource = groovySource.replace("{{"+placeHolder.get("key")+"}}", valueToReplace);
            loop++;
          }
        }
      }
      try
      {
        // now execute it
        CompilerConfiguration conf = new CompilerConfiguration();
        GroovyShell shell = new GroovyShell(binding, conf);
        long timeBegin = System.currentTimeMillis();
        Object resultExecution =  shell.evaluate(groovySource);
        long timeEnd = System.currentTimeMillis();
        // logger.info("#### towtruckCustomPage:Result ="+result);
        if (resultExecution==null)
          resultExecution="Script was executed with success, but do not return any result.";
        result.put("result", resultExecution);
        listEvents.add( new BEvent( GROOVY_EXECUTED, "Executed in "+(timeEnd-timeBegin)+" ms"));
      }
      catch(Exception e)
      {
        listEvents.add( new BEvent( GROOVY_EXECUTION_FAILED, e, ""));
        result.put("exception", "Error "+e.getMessage());
        
      }
    }
    result.put("listevents", BEventFactory.getHtml(listEvents));
    
    return result;    
    }
  
   /**
    * to test it
    * @param args
    */
  public static void main(final String[] args) {
    
    Map<String,Object> result = getGroovyMaintenance(null, "Ping");
    System.out.println("Result = "+result);
  }
}
