package com.fortumo.ws;


import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

/**
 * The servlet class that handles the requests received by the application server. Note that the number of concurrent
 * requests that this servlet handles depends on the number of connections the backing application server is
 * configured with.
 */
public class SumServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SumServlet.class);

    /**
     * Formats the value so that the decimal places are ignored if the value is an integer or long.
     */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.###");

    /**
     * The service that processes the received requests.
     */
    private final SumService service;

    public SumServlet() {
        this.service = createService();
    }

    /**
     * VisibleForTesting.
     * @return the service object.
     */
    SumService createService() {
        return new SumService();
    }

    /**
     * Handles the POST:/ request with body containing a number or the string 'end'. After validating the request body,
     * it is sent to the {@link #service} for processing. Note that the end result is formatted using the
     * {@link #DECIMAL_FORMAT}.
     *
     * @param req request object to receive the client's request.
     * @param resp response object to send response.
     * @throws IOException if I/O stream cannot be processed correctly.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String body = req.getReader().readLine();
            LOG.info("recv request={}", body);
            validate(body);
            double result;
            if (body.equals("end")) {
                result = service.doEnd();
            } else {
                double number = Double.parseDouble(body);
                result = service.doAdd(number);
            }
            resp.setStatus(200);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getOutputStream().println(DECIMAL_FORMAT.format(result));
        } catch (IllegalArgumentException e) {
            LOG.error(e.getMessage());
            resp.sendError(SC_BAD_REQUEST, "request may only contain string 'end' or number");
        } catch (Exception e) {
            LOG.error(e.getMessage());
            resp.sendError(SC_INTERNAL_SERVER_ERROR, "unexpected server error");
        }
    }

    /**
     * Validates the request body.
     * @param body the request body to validate.
     */
    private void validate(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body must not be null or blank");
        }
    }
}