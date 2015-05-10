package server;
//http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests

import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.net.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public abstract class ServerCom{

	protected ServerCom(){}
	
	public enum Method{
		GET,
		POST,
		DELETE,
		PUT;
	}
	
	protected String m_mainUrl = null;
	
    public static final String CHARSET = "UTF-8";

    /** Default connection timeout is 2.5 secs*/
    public int CONNECTION_TIMEOUT_MS = 2500;
    
    /** if the connection needs any special request properties to be added, include them in this map
     * NOTE: regarding to http://programmers.stackexchange.com/questions/162643/why-is-clean-code-suggesting-avoiding-protected-variables
     * 1) descendant class actually does stuff with this member.
     * 2) inheritors can forget about this member variable and wrong will happen
     * 3) same as 2, we make an assumption that the inheritor may forget about this member, that doesn't modify this class' behavior/functionality
     * 4) we do lose a bit of responsibility over this variable but its state is irrelevant for this class */
    protected Map<String, String> m_requestProperties = null;
    
    /**Check if we are connected a network regardless if it's wifi's or mobile's
     * @return true if we are connected to a network, otherwise false     */
    public abstract boolean isNetworkAvailable();
    
    /** create a new connection
     * @return an HttpURLConnection or null if there is no network available
     * @throws Exception      */
    public HttpURLConnection openConnection()throws Exception{
    	return openConnection(Method.GET,m_mainUrl,buildQuery(null));//we leave buildQuery method to remove ambiguousness
    }

    /** create a new connection
     * @params query:   Map containing pairs of properties and values to be added to the connection as query string
     * @return an HttpURLConnection or null if there is no network available
     * @throws Exception     */
    public HttpURLConnection openConnection( Map<String,String> queryMap)throws Exception{
    	return openConnection(Method.GET,m_mainUrl,buildQuery(queryMap));
    }
    
    /** create a new connection
     * @params int method:  HTTP method to use
     * @params query:   Map containing pairs of properties and values to be added to the connection as query string
     * @return an HttpURLConnection or null if there is no network available
     * @throws Exception     */
    public HttpURLConnection openConnection(Method method, Map<String,String> queryMap)throws Exception{
    	return openConnection(method,m_mainUrl,buildQuery(queryMap));
    }
    
    /** create a new connection
     * @params method:  HTTP method to use
     * @params query:    string containing the query
     * @return an HttpURLConnection or null if there is no network available
     * @throws Exception     */
    public HttpURLConnection openConnection(Method method, String query)throws Exception{
    	return openConnection(method,m_mainUrl,query);
    }
    
    /** create a new connection
     * @params method:  HTTP method to use
     * @params url:	String containing the url
     * @params query:   Map containing pairs of properties and values to be added to the connection as query string
     * @return an HttpURLConnection or null if there is no network available
     * @throws Exception     */
    public HttpURLConnection openConnection(Method method, String url, Map<String,String> queryMap)throws Exception{
    	return openConnection(method,url,buildQuery(queryMap));
    }
    
    /** create a new connection
     * @params method:    0 for no query, 1 for metod GET, 2 for metod POST
     * @params url:    string containing the url
     * @params query:    string containing the query
     * @return an HttpURLConnection or null if there is no network available
     * @throws Exception     */
    public HttpURLConnection openConnection(Method method, String url, String query)throws Exception{
        if( !isNetworkAvailable() )
            return null;

        if (!(url.startsWith("http") || url.startsWith("https")))
        	url = "http://" + url;

        HttpURLConnection conn = null;
        //set the type of request
        switch(method){
        case GET:
            conn = (HttpURLConnection) new URL(url+"?"+(query!=null?query:"")).openConnection();
            conn.setRequestMethod("GET");
            addRequestProperties(conn);
            conn.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            break;
        case POST:
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true); // this set POST method
            addRequestProperties(conn);
            conn.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            OutputStream output=null;
            try{//write the query
                output = conn.getOutputStream();
                output.write(query.getBytes(CHARSET));
                output.close();
            }catch(IOException ex){    System.out.println(ex.getMessage());}
            break;
        case PUT:
        	//TODO
        	break;
        case DELETE:
        	//TODO
        	break;
        default:
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(true);
            addRequestProperties(conn);
            conn.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            break;
        }
        return conn;
    }
    
    /** Iterate through the map to create a encoded and formated query string
     * @params Map query:   Map containing pairs of properties and values to be added to the connection as query string
     * @return Query string formated and encoded*/
    public final String buildQuery(Map<String, String> queryMap){
    	if (queryMap == null)
    		return null;
    	
    	StringBuilder sb = new StringBuilder();
    	Iterator<Map.Entry<String, String>> it = queryMap.entrySet().iterator();
    	boolean hasNext = it.hasNext();
		while (hasNext){
			Map.Entry<String,String> pair = it.next();
			hasNext = it.hasNext();
			try {
				sb.append(URLEncoder.encode(pair.getKey(),CHARSET)).append('=').append((URLEncoder.encode(pair.getValue(),CHARSET)));
				if(hasNext)
					sb.append('&');
			} catch (UnsupportedEncodingException e) {
				// Do nothing, since UTF-8 charset is fully supported by every virtual machine.
			}
		}
    	return sb.toString();
    }

    /** Iterate through the map of properties and add them to the connection request 
     * @params conn   recently open HttpURLConnection to add requests properties.
     * @return Query string formated and encoded*/
    private final void addRequestProperties(HttpURLConnection conn){
    	if(m_requestProperties != null){
    		Iterator<Map.Entry<String, String>> it =  m_requestProperties.entrySet().iterator();
    		while (it.hasNext()){
    			Map.Entry<String,String> pair = it.next();
    			try {
    				conn.addRequestProperty(URLEncoder.encode(pair.getKey(),CHARSET), URLEncoder.encode(pair.getValue(),CHARSET));
    			} catch (UnsupportedEncodingException e) {
    				// Do nothing, since UTF-8 charset is fully supported by every virtual machine.
    			}
    		}
    	}
    }



    /**Get a response from a httpURLconnection as String
     * @param conn an opened connection
     * @return the servers response or null if no connection exists
     * @throws Exception     */
    public String getResponse(HttpURLConnection conn)throws Exception{
        if ( conn == null )
            return null;

        StringBuffer result = new StringBuffer("");
        Scanner reader;
        //get the response and append it
        reader = new Scanner(conn.getInputStream());
        while (reader.hasNextLine()) {
            result.append(reader.nextLine());
        }
        reader.close();
        conn.disconnect();
        conn = null;

        return result.toString();
    }


    /**Connect to a server by its URL using GET method, obviously the URL
     * should have the necessary query parameters included.
     * @param mUrl servers URL
     * @return a string containing the servers response or null if no network is available
     * @throws Exception */
    public String getResponse(String mUrl) throws Exception    {
        if( !isNetworkAvailable() )
            return null;
        HttpURLConnection conn;
        Scanner rd;
        StringBuilder response=new StringBuilder();
        try {
            conn = (HttpURLConnection) new URL(mUrl).openConnection();
            conn.setRequestMethod("GET");
            rd = new Scanner(conn.getInputStream());
            while (rd.hasNextLine()) {
                response.append(rd.nextLine());
            }
            rd.close();
        } catch (Exception e) {
            throw e;
        }
        return response.toString();
    }


    /** print useful information about the given HttpURLConnection*/
    public void printConnProps(HttpURLConnection conn)throws IOException{
        System.out.println("method: "+conn.getRequestMethod());
        System.out.println("response code: "+conn.getResponseCode());
        System.out.println("response Message: "+conn.getResponseMessage());
        System.out.println("content type: "+conn.getContentType());
        System.out.println("content length: "+conn.getContentLength());
        System.out.println("content: "+(String)conn.getContent().toString());
        System.out.println("header field: "+conn.getHeaderFields());
        System.out.println("Url: "+conn.getURL());
        System.out.println("connection: "+(String)conn.toString());
        System.out.println("\n");
    }

 /* This method is for practice and test only, it has no real funcitonality
 * @param methodType
 * @param Info
 * @return */
/*
    @SuppressWarnings("unused")
    private static String ApacheREST(int methodType, String Info){

        HttpClient httpClient = new DefaultHttpClient();

        switch(methodType){
        case 0:
            HttpPost post = new HttpPost("http://10.0.2.2:2731/Api/Clientes/Cliente");
            post.setHeader("content-type", "application/json");
            try{
                JSONObject dato = new JSONObject();//build request JSON
                dato.put("info", Info);
                StringEntity entity = new StringEntity(dato.toString());
                post.setEntity(entity);// we add it to the post request
                HttpResponse resp = httpClient.execute(post);//get server response
                String respStr = EntityUtils.toString(resp.getEntity());
                return respStr;
            }
            catch(Exception ex){Log.e("ServicioRest","Error!", ex);    }
            break;
        case 1:
            HttpPut put = new HttpPut("http://10.0.2.2:2731/Api/Clientes/Cliente");
            put.setHeader("content-type", "application/json");
            try{
                //Construimos el objeto cliente en formato JSON
                JSONObject dato = new JSONObject();

                dato.put("Info", Info);
                StringEntity entity = new StringEntity(dato.toString());
                put.setEntity(entity);

                HttpResponse resp = httpClient.execute(put);
                String respStr = EntityUtils.toString(resp.getEntity());
                return respStr;
            }
            catch(Exception ex){Log.e("ServicioRest","Error!", ex);    }
            break;
        case 2:

            HttpDelete del = new HttpDelete("http://10.0.2.2:2731/Api/Clientes/Cliente/12");
            del.setHeader("content-type", "application/json");
            try{
               HttpResponse resp = httpClient.execute(del);
               String respStr = EntityUtils.toString(resp.getEntity());
               return respStr;
            }
            catch(Exception ex){Log.e("ServicioRest","Error!", ex);    }
            break;

        case 3:
            HttpGet get = new HttpGet("http://10.0.2.2:2731/Api/Clientes/Cliente/15");
             get.setHeader("content-type", "application/json");
             try{
                HttpResponse resp = httpClient.execute(get);
                String respStr = EntityUtils.toString(resp.getEntity());
                JSONObject respJSON = new JSONObject(respStr);
                int idCli = respJSON.getInt("Id");
                String nombCli = respJSON.getString("Nombre");
                int telefCli = respJSON.getInt("Telefono");
             }
            catch(Exception ex){   Log.e("ServicioRest","Error!", ex);    }
            break;
        }
        return null;
    }
*/


/*public static uploadFile(){
        String param = "value";
        File textFile = new File("/path/to/file.txt");
        File binaryFile = new File("/path/to/file.bin");
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        PrintWriter writer = null;
        try {
            OutputStream output = connection.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(output, charset), true); // true = autoFlush, important!

            // Send normal param.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
            writer.append(CRLF);
            writer.append(param).append(CRLF).flush();

            // Send text file.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"textFile\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
            writer.append(CRLF).flush();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile), charset));
                for (String line; (line = reader.readLine()) != null;) {
                    writer.append(line).append(CRLF);
                }
            } finally {
                if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
            }
            writer.flush();

            // Send binary file.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            InputStream input = null;
            try {
                input = new FileInputStream(binaryFile);
                byte[] buffer = new byte[1024];
                for (int length = 0; (length = input.read(buffer)) > 0;) {
                    output.write(buffer, 0, length);
                }
                output.flush(); // Important! Output cannot be closed. Close of writer will close output as well.
            } finally {
                if (input != null) try { input.close(); } catch (IOException logOrIgnore) {}
            }
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of binary boundary.

            // End of multipart/form-data.
            writer.append("--" + boundary + "--").append(CRLF);
        } finally {
            if (writer != null) writer.close();
        }
    }
 */

}