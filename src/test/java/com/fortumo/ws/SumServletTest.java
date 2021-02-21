package com.fortumo.ws;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Supplier;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

class SumServletTest {

    /**
     * tests when request object is null then, an error is sent.
     */
    @Test
    void whenRequestIsNull_thenSendErrorCalled() throws IOException {
        HttpServletResponse respMock = response();
        servlet().doPost(null, respMock);
        verifySendErrorCalled(respMock);
    }

    /**
     * tests when either request or response object is null, then an exception is thrown.
     */
    @Test
    void whenRequestOrResponseIsNull_thenThrowsException() {
        assertThrows(NullPointerException.class, () -> servlet().doPost(mock(HttpServletRequest.class), null));
        assertThrows(NullPointerException.class, () -> servlet().doPost(null, null));
    }

    /**
     * tests when request body received is null, then an error is sent.
     */
    @Test
    void whenRequestBodyIsNull_thenSendErrorCalled() throws IOException {
        HttpServletResponse respMock = response();
        servlet().doPost(request(() -> null), respMock);
        verifySendErrorCalled(respMock, SC_BAD_REQUEST);
    }

    /**
     * tests when request body received is blank, then an error is sent.
     */
    @Test
    void whenRequestBodyIsBlank_thenSendErrorCalled() throws IOException {
        HttpServletResponse respMock = response();
        servlet().doPost(request(() -> " "), respMock);
        verifySendErrorCalled(respMock, SC_BAD_REQUEST);
    }

    /**
     * tests when request body received is neither a number nor 'end' string, then an error is sent.
     */
    @Test
    void whenRequestBodyIsNotNumberOrEnd_thenSendErrorCalled() throws IOException {
        HttpServletResponse respMock = response();
        servlet().doPost(request(() -> "some-arbitrary-string"), respMock);
        verifySendErrorCalled(respMock, SC_BAD_REQUEST);
    }

    /**
     * tests when service throws a {@link ServiceException}, then an error is sent.
     */
    @Test
    void whenServiceThrowsServiceException_thenSendErrorCalled() throws IOException {
        HttpServletResponse respMock = response();
        servlet(() -> new ServiceException(null)).doPost(request(() -> "1"), respMock);
        verifySendErrorCalled(respMock, SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * tests when service throws any unexpected exception, then an error is sent.
     */
    @Test
    void whenServiceThrowsException_thenSendErrorCalled() throws IOException {
        HttpServletResponse respMock = response();
        servlet(RuntimeException::new).doPost(request(() -> "1"), respMock);
        verifySendErrorCalled(respMock, SC_INTERNAL_SERVER_ERROR, "unexpected server error");
    }

    /**
     * tests the success scenario when request body contains a number.
     */
    @Test
    void whenNumberReceived_thenSuccess() throws IOException {
        HttpServletResponse respMock = response();
        servlet().doPost(request(() -> "1"), respMock);
        verifySuccessResponse(respMock);
    }

    /**
     * tests the success scenario when request body contains the string 'end'.
     */
    @Test
    void whenEndReceived_thenSuccess() throws IOException {
        HttpServletResponse respMock = response();
        servlet().doPost(request(() -> "end"), respMock);
        verifySuccessResponse(respMock);
    }

    /**
     * Creates the {@link SumServlet} to test. A service is injected that returns an arbitrary number when its apply
     * method is called.
     * @return the servlet to test.
     */
    SumServlet servlet() {
        return servlet(null);
    }

    /**
     * @param serviceExceptionSupplier exception to be thrown by the service.
     * @return the servlet to test that has a service that throws the exception supplied.
     */
    SumServlet servlet(Supplier<Throwable> serviceExceptionSupplier) {
        return new SumServlet() {
            @Override
            SumService createService() {
                try {
                    SumService serviceMock = mock(SumService.class);
                    if (serviceExceptionSupplier != null) {
                        when(serviceMock.doEnd()).thenThrow(serviceExceptionSupplier.get());
                        when(serviceMock.doAdd(anyLong())).thenThrow(serviceExceptionSupplier.get());
                    } else {
                        when(serviceMock.doAdd(anyLong())).thenReturn(10L);
                    }
                    return serviceMock;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Creates a mock {@link HttpServletRequest} object that returns the string object provided by the bodySupplier
     * param.
     * @param bodySupplier data received as part of the request.
     * @return request mock.
     */
    HttpServletRequest request(Supplier<String> bodySupplier) throws IOException {
        BufferedReader brMock = mock(BufferedReader.class);
        when(brMock.readLine()).thenReturn(bodySupplier.get());

        HttpServletRequest reqMock = mock(HttpServletRequest.class);
        when(reqMock.getReader()).thenReturn(brMock);

        return reqMock;
    }

    /**
     * Creates a mock {@link HttpServletResponse} object which will be used for method call verifications.
     * @return response mock.
     */
    HttpServletResponse response() throws IOException {
        ServletOutputStream osMock = mock(ServletOutputStream.class);
        doNothing().when(osMock).println(anyLong());
        HttpServletResponse respMock = mock(HttpServletResponse.class);
        when(respMock.getOutputStream()).thenReturn(osMock);
        return respMock;
    }

    /**
     * Verifies the response mock method calls in case of success scenario.
     * @param respMock response mock to verify.
     */
    void verifySuccessResponse(HttpServletResponse respMock) throws IOException {
        verify(respMock).setStatus(200);
        verify(respMock).setContentType(anyString());
        verify(respMock).getOutputStream();
        verify(respMock.getOutputStream()).println(anyLong());
    }

    void verifySendErrorCalled(HttpServletResponse respMock) throws IOException {
        verify(respMock).sendError(anyInt(), anyString());
    }

    void verifySendErrorCalled(HttpServletResponse respMock, int status) throws IOException {
        verify(respMock).sendError(eq(status), anyString());
    }

    /**
     * Verifies the {@link HttpServletResponse#sendError(int, String)} calls when an error needs to be sent to the client.
     * @param respMock response mock to verify.
     * @param status   HTTP status code to verify.
     * @param msg      error message to verify.
     */
    void verifySendErrorCalled(HttpServletResponse respMock, int status, String msg) throws IOException {
        verify(respMock).sendError(eq(status), eq(msg));
    }

}