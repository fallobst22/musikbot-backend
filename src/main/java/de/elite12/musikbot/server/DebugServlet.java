package de.elite12.musikbot.server;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DebugServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3526105087309818816L;
	private Controller control;

    public DebugServlet(Controller ctr) {
        this.control = ctr;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    		throws ServletException, IOException {
    	if (req.getSession().getAttribute("user") != null && ((User) req.getSession().getAttribute("user")).isAdmin()) {
            req.setAttribute("worked", Boolean.valueOf(true));
            req.setAttribute("control", this.control);
            req.getRequestDispatcher("/debug.jsp").forward(req, resp);
            return;
        }
        resp.sendError(404);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    		throws ServletException, IOException {
    	if (req.getSession().getAttribute("user") != null && ((User) req.getSession().getAttribute("user")).isAdmin()) {
    		Enumeration<String> e = req.getParameterNames();
    		while(e.hasMoreElements()) {
    			switch(e.nextElement()) {
    				case "rclient": {
    					try {
    						this.control.getConnectionListener().getHandle().sendShutdown();
    					}
    					catch(NullPointerException err) {
    						
    					}
    					break;
    				}
    				case "rserver": {
    					new Timer().schedule(new TimerTask() {
							
							@Override
							public void run() {
								System.exit(0);
							}
						}, 2500);
    					break;
    				}
    			}
    		}
            resp.sendRedirect("/debug/");
    		return;
    	}
    	resp.sendError(404);
    }
}