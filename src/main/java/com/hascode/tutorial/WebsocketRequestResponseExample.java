package com.hascode.tutorial;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class WebsocketRequestResponseExample {

  public static void main(String[] args) {
    final int port = 7777;

    RSocket responseHandler = new AbstractRSocket() {
      @Override
      public Mono<Payload> requestResponse(Payload payload) {
        return Mono
            .just(DefaultPayload
                .create(String.format("request-response: %s", payload.getDataUtf8())));
      }
    };

    Disposable server = RSocketFactory.receive()
        .acceptor(
            (setupPayload, rsocket) ->
                Mono.just(responseHandler))
        .transport(WebsocketServerTransport.create("localhost", port))
        .start()
        .subscribe();

    System.out.printf("websocket server started on port %d%n", port);

    RSocket socket =
        RSocketFactory.connect()
            .transport(WebsocketClientTransport.create("localhost", port))
            .start()
            .block();

    System.out.printf("websocket client initialized, connecting to port %d%n", port);

    socket
        .requestResponse(DefaultPayload.create("request-response message"))
        .map(Payload::getDataUtf8)
        .doOnNext(System.out::println)
        .block();

    socket.dispose();
    server.dispose();
  }
}
