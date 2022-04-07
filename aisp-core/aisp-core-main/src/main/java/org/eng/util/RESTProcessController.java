/*******************************************************************************
 * Copyright [2022] [IBM]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eng.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eng.ENGLogger;

public class RESTProcessController  extends AbstractProcessController implements IProcessController {

	transient Server server;
	protected final int port;
	
	private static Map<Integer, RESTProcessController> controllers = new HashMap<Integer, RESTProcessController>();
	
	public static RESTProcessController getController(int port) {
		return controllers.get(port);
	}
	
	public RESTProcessController(int port) {
		super(ENGLogger.logger);
		if (controllers.get(port) != null)
			throw new IllegalArgumentException("Instance with port " + port + " already exists.");
		this.port = port;
		controllers.put(port, this);
	}

	@Override
	public boolean start() throws Exception {
		if (server == null)
			server = startServer(port);
		return true;
	}

	@Override
	public void stop() {
		if (server != null) {
			try {
				this.server.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.server = null;
		}
		
	}

    public static class StartServlet extends HttpServlet
    {
    	private static final long serialVersionUID = 7121096731778149040L;

		public StartServlet() { }
    	
        @Override
        protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        	RESTProcessController rpc = RESTProcessController.getController(request.getServerPort());
        	if (rpc == null)
        		throw new ServletException("Could not find controller to notify of start request");
        	rpc.notifyListeners(null, true);		// Call the listeners start method.
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("");
        }
    }
    
    public static class StopServlet extends HttpServlet
    {
    	private static final long serialVersionUID = 8742043825370476580L;

		public StopServlet() { }
    	
        @Override
        protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        	RESTProcessController rpc = RESTProcessController.getController(request.getServerPort());
        	if (rpc == null)
        		throw new ServletException("Could not find controller to notify of start request");
        	rpc.notifyListeners(null, false);		// Call the listeners stop method.
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("");
        }
    }

	private Server startServer(int port) throws Exception {

        // Create a basic jetty server object that will listen on the given port. 
        // Note that if you set this to port 0 then a randomly available port
        // will be assigned that you can either look in the logs for the port,
        // or programmatically obtain it for use in test cases.
        Server server = new Server(port);

        // The ServletHandler is a dead simple way to create a context handler
        // that is backed by an instance of a Servlet.
        // This handler then needs to be registered with the Server object.
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        // Passing in the class for the Servlet allows jetty to instantiate an
        // instance of that Servlet and mount it on a given context path.

        // IMPORTANT:
        // This is a raw Servlet, not a Servlet that has been configured
        // through a web.xml @WebServlet annotation, or anything similar.
        handler.addServletWithMapping(StartServlet.class, "/start");
        handler.addServletWithMapping(StopServlet.class,  "/stop");

        // Start things up!
        server.start();

        // The use of server.join() the will make the current thread join and
        // wait until the server is done executing.
        // See http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#join()
//        System.out.println("Classify server started on port " + port + ".");
        ServletMapping[] mappings = handler.getServletMappings();
        for (ServletMapping sm : mappings) {
        	System.out.println(sm);
        }
//        server.join();
        
        return server;
	}
	


}
