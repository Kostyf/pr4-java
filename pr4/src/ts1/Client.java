package ts1;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class Client {

    private static final Logger log = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    static {
        log.setLevel(Level.INFO);
    }

    public static void main(String[] args) {
        final Client client = new Client();
        final RSocket rSocket = client.connect();

        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        final JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(300, 150));
        panel.setBackground(Color.lightGray);
        panel.setLayout(new GridLayout(2, 2, 10, 10));

        final JButton fireAndForgetBtn = new JButton("Fire and Forget");
        final JButton requestResponseBtn = new JButton("Request-Response");
        final JButton requestStreamBtn = new JButton("Request-Stream");
        final JButton requestChannelBtn = new JButton("Request-Channel");

        final ActionListener AListener = e -> {
            switch (e.getActionCommand()) {
                case "Fire and Forget" -> client.fireAndForget(rSocket);
                case "Request-Response" -> client.requestResponse(rSocket);
                case "Request-Stream" -> client.requestStream(rSocket);
                case "Request-Channel" -> client.requestChannel(rSocket);
            }
        };

        fireAndForgetBtn.addActionListener(AListener);
        requestResponseBtn.addActionListener(AListener);
        requestStreamBtn.addActionListener(AListener);
        requestChannelBtn.addActionListener(AListener);

        panel.add(fireAndForgetBtn);
        panel.add(requestResponseBtn);
        panel.add(requestStreamBtn);
        panel.add(requestChannelBtn);

        frame.add(panel);
        frame.setVisible(true);
    }

    private RSocket connect() {
        return RSocketFactory.connect()
                .transport(WebsocketClientTransport.create(8801))
                .start()
                .block();
    }

    private void fireAndForget(RSocket rSocket) {
        log.info("sending fire and forget from client");
        Flux.just(new Message("fire and forget JAVA client!"))
                .map(MessageMapper::messageToJson)
                .map(DefaultPayload::create)
                .flatMap(rSocket::fireAndForget)
                .blockLast();
    }

    private void requestResponse(RSocket rSocket) {
        log.info("sending request-response from client");
        Flux.just(new Message("requestResponse from JAVA client!"))
                .map(MessageMapper::messageToJson)
                .map(DefaultPayload::create)
                .flatMap(rSocket::requestResponse)
                .map(Payload::getDataUtf8)
                .doOnNext(payloadString -> {
                    log.info("got response in JAVA client");
                    log.info(payloadString);
                })
                .blockLast();
    }

    private void requestStream(RSocket rSocket) {
        log.info("sending request-stream from client");
        Flux.just(new Message("requestStream from JAVA client!"))
                .map(MessageMapper::messageToJson)
                .map(DefaultPayload::create)
                .flatMap(rSocket::requestStream)
                .map(Payload::getDataUtf8)
                .doOnNext(log::info)
                .blockLast();
    }

    private void requestChannel(RSocket rSocket) {
        log.info("sending request-channel from client");
        final Flux<Payload> requestPayload = Flux.range(0, 5)
                .map(count -> new Message("requestChannel from JAVA client! #" + count))
                .map(msg -> {
                    log.info("sending message: {}", msg.message);
                    return MessageMapper.messageToJson(msg);
                })
                .map(DefaultPayload::create);

        rSocket
                .requestChannel(requestPayload)
                .map(Payload::getDataUtf8)
                .doOnNext(log::info)
                .blockLast();
    }
}
