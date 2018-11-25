package com.hascode.tutorial;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class TcpRequestResponseExample {

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
        .transport(TcpServerTransport.create("localhost", port))
        .start()
        .subscribe();

    System.out.printf("tcp server started on port %d%n", port);

    RSocket socket =
        RSocketFactory.connect()
            .transport(TcpClientTransport.create("localhost", port))
            .start()
            .block();

    System.out.printf("tcp client initialized, connecting to port %d%n", port);

    socket
        .requestResponse(DefaultPayload.create("request-response message"))
        .map(Payload::getDataUtf8)
        .doOnNext(System.out::println)
        .block();

    socket.dispose();
    server.dispose();
  }
}
