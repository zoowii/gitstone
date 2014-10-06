package com.zoowii.mvc.http;

import java.io.IOException;
import java.nio.channels.Channel;

/**
 * TODO
 * Created by zoowii on 14/10/6.
 */
public class HttpChannel implements Channel {
    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }
}
