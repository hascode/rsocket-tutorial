package com.hascode.tutorial;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WebsocketRequestStreamExample {

  public static void main(String[] args) throws Exception {
    final int port = 7777;

    RSocket responseHandler = new AbstractRSocket() {
      @Override
      public Flux<Payload> requestStream(Payload payload) {
        return Flux.range(1, 20)
            .map(i -> DefaultPayload.create("part-" + i));
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
        .requestStream(DefaultPayload.create("request-stream message"))
        .subscribe(payload -> System.out.println(payload.getDataUtf8()));

    Thread.sleep(3000);
    socket.dispose();
    server.dispose();
  }
}
