package com.fortumo.ws;


import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.function.Function;

public class SumServlet extends HttpServlet {

    private final Function<String, Long> service;

    public SumServlet() {
        this.service = new SumService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = req.getReader().readLine();
        Long result = service.apply(body);
        try (ServletOutputStream os = resp.getOutputStream()) {
            os.println(result);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain;charset=UTF-8");
        var out = response.getOutputStream();
        out.print("use POST endpoint with a number or 'end' string. see documentation");
    }
}