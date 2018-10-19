package nl.knmi.adaguc.services.esgfsearch;

import java.io.IOException;
import java.util.Calendar;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.adaguc.tools.WebRequestBadStatusException;


public class THREDDSCatalogToHTML {
	private String getHTTPParamNoExceptionButNull(HttpServletRequest request, String name){
		try {
			return HTTPTools.getHTTPParam(request, name);
		} catch (Exception e) {
		}
		return null;
	}
	class BuildHTMlResult{
	    String result;
	    int rn;
	  }

	  BuildHTMlResult buildHTML(JSONArray array,String root,int oddEven,String openid,int rn){
	    if(array == null)return null;
	    StringBuffer html = new StringBuffer();
	    try {
	      //Try to get the catalogURL 

	      for(int j=0;j<array.length();j++){
	        rn++;
	        String opendapURL=null;
	        String httpserverURL=null;
	        String hrefURL=null;
	        String catalogURL=null;
	        String nodeText = null;
	        String fileSize = "";
	        JSONObject a=array.getJSONObject(j);
	        nodeText = a.getString("text");
	        try{opendapURL = a.getString("dapurl");}catch (JSONException e) {}
	        try{httpserverURL = a.getString("httpurl");}catch (JSONException e) {}
	        try{catalogURL = a.getString("catalogurl");}catch (JSONException e) {}
	        
	        try{opendapURL = a.getString("opendap");}catch (JSONException e) {}
	        try{httpserverURL = a.getString("httpserver");}catch (JSONException e) {}
	        try{catalogURL = a.getString("catalogURL");}catch (JSONException e) {}
	        
	        try{httpserverURL = a.getString("httpurl");}catch (JSONException e) {}
	        try{catalogURL = a.getString("catalogurl");}catch (JSONException e) {}
	        try{hrefURL = a.getString("href");}catch (JSONException e) {}
	        try{fileSize = a.getString("dataSize");}catch (JSONException e) {}

	        oddEven = 1-oddEven;
	        if(oddEven==0){
	          //html+="<tr class=\"even\"><td>";
	          html.append("<tr class=\"even\"><td>"+rn+"</td><td>");
	        }else{
	          //html+="<tr class=\"odd\"><td>";
	          html.append("<tr class=\"odd\"><td>"+rn+"</td><td>");
	        }
	        if(hrefURL == null){
	          // html+=root+nodeText+"<br/>";
	          html.append(root+nodeText+"<br/>");
	        }else{
	          //html+=root+"<a href=\""+hrefURL+"\">"+nodeText+"</a>";
	          html.append(root+"<a href=\""+hrefURL+"\">"+nodeText+"</a>");
	        }

	        // html+="<td>"+dataSize+"</td>";
	        html.append("<td>"+fileSize+"</td>");
	        //html.append("<td>");html.append(dataSize);html.append("</td>");

	        String dapLink = "";
	        String httpLink = "";

	        /*
	         * Only show opendap when it is really advertised by the server.
	         */ if(opendapURL == null && httpserverURL !=null){
	           opendapURL = httpserverURL.replace("fileServer", "dodsC")+"#";
	         }

	         if(opendapURL!=null){
	           dapLink="<span class=\"link\" onclick=\"renderFileViewer({url:'"+opendapURL+"'});\">view</span>";
	           //dapLink="<a href=\"#\" onclick=\"renderFileViewer({url:'"+openDAPURL+"'});\">view</a>";
	         }
	         if(httpserverURL!=null){
	           if(openid!=null){
	             httpLink="<a class=\"c4i-wizard-catalogbrowser-downloadlinkwithopenid\"  href=\""+httpserverURL+"?openid="+openid+"\" target=\"_blank\"\">download</a>";
	           }else{
	             httpLink="<a class=\"c4i-wizard-catalogbrowser-downloadlinknoopenid\" href=\""+httpserverURL+"\">download</a>";
	           }
	         }
	         //html+="</td><td>"+dapLink+"</td><td>"+httpLink;
	         html.append("</td><td>");html.append(dapLink);html.append("</td><td>");html.append(httpLink);
	         html.append("</td>");
	         if(httpserverURL == null && opendapURL == null && catalogURL == null){
	           //html+="</td><td>-";  
	           html.append("<td></td>");
	         }else{
	           html.append("<td><span onclick=\"basket.postIdentifiersToBasket({id:'"+nodeText+"',httpurl:'"+httpserverURL+"',dapurl:'"+opendapURL+"',catalogurl:'"+catalogURL+"',"+"filesize:'"+fileSize+"'});\" class=\"shoppingbasketicon\"/></td>\n");
	         }


	         //html+="</td></tr>";
	         html.append("</tr>");

	         try{
	           JSONArray children = a.getJSONArray("children");
	           //html+=buildHTML(children,root+"&nbsp;&nbsp;&nbsp;&nbsp;",oddEven);
	           BuildHTMlResult b=buildHTML(children,root+"&nbsp;&nbsp;&nbsp;&nbsp;",1-oddEven,openid,rn);
	           html.append(b.result);
	           rn=b.rn;
	         } catch (JSONException e) {
	         }
	      }
	    } catch (JSONException e) {
	    }

	    BuildHTMlResult b=new BuildHTMlResult();
	    b.result=html.toString();
	    b.rn=rn;
	    return b;
	  }
	public void handleCatalogBrowserRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Debug.println("SERVICE CATALOGBROWSER: "+request.getQueryString());
		String format= getHTTPParamNoExceptionButNull(request,"format");
		JSONResponse jsonResponse = new JSONResponse(request);
		if(format==null)format="";
		boolean flat = false;
		try{
			String variableFilter="",textFilter = "";
			try{
				variableFilter = HTTPTools.getHTTPParam(request, "variables");
			}catch(Exception e){
			}

			try{
				textFilter = HTTPTools.getHTTPParam(request, "filter");
			}catch(Exception e){
			}
			try{
				String mode= HTTPTools.getHTTPParam(request, "mode");
				if(mode.equals("flat")){
					flat=true;
				}
			}catch(Exception e){
			}


			if(variableFilter.length()==0){
				/* Try to obtain variable filter from catalog url */
				try{
					String catalogURL= HTTPTools.getHTTPParam(request, "node");
					String[] s = catalogURL.split("#");
					Debug.println(""+s.length);
					if(s.length == 2){
						if(s[1].length()>0)
							variableFilter = s[1];
					}
				}catch(Exception e){

				}

			}

			HTTPTools.validateInputTokens(variableFilter);
			HTTPTools.validateInputTokens(textFilter);

			Debug.println("Starting CATALOG: "+request.getQueryString());
			long startTimeInMillis = Calendar.getInstance().getTimeInMillis();
			JSONArray treeElements = null;
			try{
				treeElements = THREDDSCatalogBrowser.browseThreddsCatalog(request, variableFilter,textFilter); 
			}catch(Exception e){
				jsonResponse.setException("Unable to read catalog", e);
				try {
					jsonResponse.print(response);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				return;

			}

			long stopTimeInMillis = Calendar.getInstance().getTimeInMillis();
			Debug.println("Finished CATALOG: "+" ("+(stopTimeInMillis-startTimeInMillis)+" ms) "+request.getQueryString());

			if(treeElements == null){
				jsonResponse.setErrorMessage("Unable to read catalog", 200);
				try {
					jsonResponse.print(response);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				return;

			}




			if(jsonResponse.hasError()==false){
				if(flat == false){


					String html="";
					String catalogURL= HTTPTools.getHTTPParam(request, "node");
					html+="<b>Catalog url:</b> <a target=\"_blank\" href=\""+catalogURL+"\">"+catalogURL+"</a></hr>";
					/*for(int j=0;j<treeElements.getJSONObject(0).getJSONArray("children").length();j++){
		                html+="j="+treeElements.getJSONObject(0).getString("text")+"<br/>"; 
		              } */

					try{
						Vector<String> availableVars = new Vector<String>();
						try{
							JSONArray variablesToChoose = treeElements.getJSONObject(0).getJSONArray("variables");
							for(int j=0;j<variablesToChoose.length();j++){
								availableVars.add(variablesToChoose.getJSONObject(j).getString("name"));
								//DebugConsole.println("a "+variablesToChoose.getJSONObject(j).getString("name"));
							}
						}catch(Exception e){

						}


						Debug.println("variableFilter: '"+variableFilter+"'");
						Debug.println("textFilter: '"+textFilter+"'");
						html+="<div class=\"c4i-catalogbrowser-variableandtextfilter\"><form  class=\"varFilter\">";
						if(availableVars.size()>0){
							html+="<b>Variables:</b>";
							for(int j=0;j<availableVars.size();j++){
								if(j!=0)html+="&nbsp;";
								String checked="";//checked=\"yes\"";
								if(variableFilter.length()>0){
									checked="";
									if(availableVars.get(j).matches(variableFilter))checked="checked=\"yes\"";
								}
								html+="<input type=\"checkbox\" name=\"variables\" id=\""+availableVars.get(j)+"\" "+checked+">"+availableVars.get(j);
							}
							html+="<br/><br/>";
						}
						html+="<b>Text filter:</b> <input type=\"textarea\" class=\"textfilter\" id=\"textfilter\" value=\""+textFilter+"\" ></input>";

						//html+="<br/>";
						//html+="&nbsp; <input style=\"float:right;\" type=\"button\" value=\"Go\" onclick=\"setVarFilter();\"/>";

						html+="</form></div>";

					}catch(Exception e){
						e.printStackTrace();
					}

					html+="<div id=\"datasetfilelist\"><table class=\"c4i-catalogbrowser-table\">";
					html+="<tr>";
					html+="<th>#</th>";
					html+="<th width=\"100%\" class=\"c4i-catalogbrowser-th\">Resource title</th>";
					html+="<th class=\"c4i-catalogbrowser-th\"><b>Size</b></th>";
					html+="<th class=\"c4i-catalogbrowser-th\"><b>Opendap</b></th>";
					html+="<th class=\"c4i-catalogbrowser-th\"><b>Download</b></th>";
					html+="<th class=\"c4i-catalogbrowser-th\"><b>Basket</b></th>";
					html+="</tr>";

					long startTimeInMillis1 = Calendar.getInstance().getTimeInMillis();

					String openid = null;
					try{
						AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
						if(authenticator != null){
							try {
								openid = UserManager.getUser(authenticator).getUserId();
							} catch(Exception e){
								Debug.println("No user information provided: " + e.getMessage());
							}

						} 
					}catch(Exception e){            
					}
					html+=buildHTML(treeElements,"",0,openid,0).result+"</table></div>";
					long stopTimeInMillis1 = Calendar.getInstance().getTimeInMillis();
					Debug.println("Finished building HTML with length "+html.length() +" in ("+(stopTimeInMillis1-startTimeInMillis1)+" ms)");
					if(format.equals("text/html")){
						response.setContentType("text/html");
						response.getWriter().print(html);
					}else if(format.equals("application/json")){
						JSONObject a = new JSONObject();
						a.put("html", html);
						try{
							jsonResponse.setMessage(a);
						} catch(Exception e){
							jsonResponse.setException("Catalogbrowser error:",e);
						}
						try {
							jsonResponse.print(response);
						} catch (Exception e1) {

						}
					}else{
						try{
							jsonResponse.setMessage(treeElements.toString());
						} catch(Exception e){
							jsonResponse.setException("Catalogbrowser error:",e);
						}
						try {
							jsonResponse.print(response);
						} catch (Exception e1) {

						}
					}
				}

				if(flat == true){
					//Debug.println(treeElements.toString());
					//THREDDSCatalogBrowser.MakeFlat b = new THREDDSCatalogBrowser.MakeFlat();
					JSONArray allFilesFlat = THREDDSCatalogBrowser.makeFlat(treeElements);
					JSONObject data = new JSONObject();
					data.put("files",allFilesFlat);
					jsonResponse.setMessage(data);
					//Debug.println("Found "+allFilesFlat.length());

					try {
						jsonResponse.print(response);
					} catch (Exception e1) {

					}
				}
				Debug.println("Catalog request finished.");
			}
		}catch(WebRequestBadStatusException e){
			if(format.equals("text/html")){
				response.setContentType("text/html");
				String html="";
				if(e.getStatusCode() == 404){
					html="Catalog not found! (404)";
				}else{
					html=e.getMessage();
				}
				response.getWriter().print(html);
			}else{
				jsonResponse.setException("error", e);
				try {jsonResponse.print(response);} catch (Exception e1) {}
			}


		} catch (Exception e2) {
			e2.printStackTrace();

			if(format.equals("text/html")){
				response.setContentType("text/html");
				String html="";
				html=e2.getMessage();
				html+="<form><input type=button value=\"Refresh\" onClick=\"history.go()\"></form>";
				response.getWriter().print(html);
			}else{
				jsonResponse.setException("error", e2);
				try {jsonResponse.print(response);} catch (Exception e1) {}
			}
		}
	}

}
