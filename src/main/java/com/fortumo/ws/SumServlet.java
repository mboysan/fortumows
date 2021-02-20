package com.fortumo.ws;


import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Function;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

public class SumServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SumServlet.class);

    private final Function<String, Long> service;

    public SumServlet() {
        this.service = createService();
    }

    Function<String, Long> createService() {
        return new SumService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String body = req.getReader().readLine();
            validate(body);
            LOG.info("recv request={}", body);
            long result = service.apply(body);
            resp.setStatus(200);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getOutputStream().println(result);
        } catch (IllegalArgumentException e) {
            resp.sendError(SC_BAD_REQUEST, "request may only contain string 'end' or number");
        } catch (ServiceException e) {
            resp.sendError(SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            resp.sendError(SC_INTERNAL_SERVER_ERROR, "unexpected server error");
        }
    }

    void validate(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body must not be null or blank");
        }
    }
}