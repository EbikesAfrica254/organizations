package com.ebikes.organizations.listeners;

import java.io.IOException;

public interface IncomingEventHandler {

  void handle(byte[] payload) throws IOException;

  boolean matches(String routingKey);
}
