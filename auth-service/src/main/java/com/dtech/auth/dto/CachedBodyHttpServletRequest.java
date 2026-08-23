package com.dtech.auth.dto;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Getter
@Log4j2
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private byte[] body;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        InputStream inputStream = request.getInputStream();
        this.body = inputStream.readAllBytes();
        log.info("Body reader from cache - Auth {} ",request);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new ServletInputStream() {
            private ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);

            @Override
            public int read() throws IOException {
                log.info("Read byte from cache - Auth {} ",this);
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }

}