package com.hascode.tutorial;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import java.time.Duration;
import java.time.Instant;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WebsocketRequestChannelExample {

  public static void main(String[] args) {
    final int port = 7777;

    RSocket responseHandler = new AbstractRSocket() {
      @Override
      public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return Flux.from(payloads)
            .map(Payload::getDataUtf8)
            .map(str -> String.format("channel message received: '%s'", str))
            .map(DefaultPayload::create);
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
        .requestChannel(
            Flux.interval(Duration.ofMillis(1_000))
                .map(i -> DefaultPayload.create("channel message " + Instant.now())))
        .map(Payload::getDataUtf8)
        .doOnNext(System.out::println)
        .take(10)
        .doFinally(signalType -> socket.dispose())
        .then().block();

    server.dispose();
  }
}
