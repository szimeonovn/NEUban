package hu.unideb.inf.rft.neuban.web.handlers;

import hu.unideb.inf.rft.neuban.service.exceptions.ColumnAlreadyExistsException;
import hu.unideb.inf.rft.neuban.service.exceptions.data.DataNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalControllerExceptionHandler {

	private static final String ERROR_VIEW = "error";
	private static final String ERROR_MESSAGE_MODEL_OBJECT_NAME = "errorMessage";

	//TODO might break down for different scenarios
	@ExceptionHandler(value = {
			DataNotFoundException.class,
			ColumnAlreadyExistsException.class,
	})
	public ModelAndView defaultErrorHandler(Exception e) {
		final ModelAndView modelAndView = new ModelAndView(ERROR_VIEW);
		modelAndView.addObject(ERROR_MESSAGE_MODEL_OBJECT_NAME, e.getMessage());
		return modelAndView;
	}
}
