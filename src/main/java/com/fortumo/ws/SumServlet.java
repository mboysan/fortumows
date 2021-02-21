package com.fortumo.ws;


import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

public class SumServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SumServlet.class);

    /**
     * The service that processes the received requests.
     */
    private final SumService service;

    public SumServlet() {
        this.service = createService();
    }

    SumService createService() {
        return new SumService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String body = req.getReader().readLine();
            LOG.info("recv request={}", body);
            validate(body);
            long result;
            if (body.equals("end")) {
                result = service.doEnd();
            } else {
                long number = Long.parseLong(body);
                result = service.doAdd(number);
            }
            resp.setStatus(200);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getOutputStream().println(result);
        } catch (IllegalArgumentException e) {
            LOG.error(e.getMessage());
            resp.sendError(SC_BAD_REQUEST, "request may only contain string 'end' or number");
        } catch (Exception e) {
            LOG.error(e.getMessage());
            resp.sendError(SC_INTERNAL_SERVER_ERROR, "unexpected server error");
        }
    }

    void validate(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body must not be null or blank");
        }
    }
}