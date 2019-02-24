package de.elite12.musikbot.server.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

	private Logger logger = LoggerFactory.getLogger(ErrorController.class);
	
	@Override
	public String getErrorPath() {
		return "/error";
	}
	
	@RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
		Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
	    //Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");
	    String errormsg = (String) request.getAttribute("javax.servlet.error.message");
	    String path = (String) request.getAttribute("javax.servlet.forward.request_uri"); 
	    
		logger.warn("Error for User: " + SecurityContextHolder.getContext().getAuthentication().getPrincipal() + 
				", Error Code: " + statusCode + 
				" Message: " + errormsg + 
				" Path: " + path);
		
		model.addAttribute("code", statusCode);
		model.addAttribute("path", path);
		model.addAttribute("message", HttpStatus.resolve(statusCode).getReasonPhrase());
		
        return "error";
    }
}
