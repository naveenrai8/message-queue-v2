package com.nr.mq.publisher;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class NettyClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8085;
    private static final long PUBLISH_INTERVAL_SECONDS = 5;

    private EventLoopGroup group;
    private Channel channel;
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void start() throws Exception {
        group = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringDecoder());
                        ch.pipeline().addLast(new StringEncoder());
                        ch.pipeline().addLast(new NettyClientHandler());
                    }
                });

        ChannelFuture f = b.connect(SERVER_ADDRESS, SERVER_PORT).sync();
        channel = f.channel();
        log.info("Netty client connected to {}:{}", SERVER_ADDRESS, SERVER_PORT);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::sendMessage, 0, PUBLISH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void sendMessage() {
        String message = "Hello from Netty Publisher";
        log.info("Sending message: {}", message);
        channel.writeAndFlush(message);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}
