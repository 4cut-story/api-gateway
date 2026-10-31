package net.java21.fourcut.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/** 하위 서비스 흉내. 받은 요청의 경로와 헤더를 기록하고, JWKS도 내준다. */
final class FakeBackend implements AutoCloseable {

  private final HttpServer server;
  private final AtomicReference<String> jwks = new AtomicReference<>("{\"keys\":[]}");
  private final AtomicReference<Map<String, String>> lastHeaders = new AtomicReference<>();
  private final AtomicReference<String> lastPath = new AtomicReference<>();
  private volatile int jwksStatus = 200;

  FakeBackend() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/.well-known/jwks.json", this::jwks);
    server.createContext("/", this::echo);
    server.start();
  }

  String url() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  void jwks(String json) {
    jwks.set(json);
  }

  void jwksStatus(int status) {
    this.jwksStatus = status;
  }

  Map<String, String> lastHeaders() {
    return lastHeaders.get();
  }

  String lastPath() {
    return lastPath.get();
  }

  void reset() {
    lastHeaders.set(null);
    lastPath.set(null);
  }

  private void jwks(HttpExchange exchange) throws IOException {
    respond(exchange, jwksStatus, jwks.get());
  }

  private void echo(HttpExchange exchange) throws IOException {
    Map<String, String> headers = new TreeMap<>();
    exchange
        .getRequestHeaders()
        .forEach((k, v) -> headers.put(k.toLowerCase(Locale.ROOT), String.join(",", v)));
    lastHeaders.set(headers);
    lastPath.set(exchange.getRequestURI().getPath());
    respond(exchange, 200, "{\"ok\":true}");
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
