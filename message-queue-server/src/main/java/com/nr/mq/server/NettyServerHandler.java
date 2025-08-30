package com.nr.mq.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@ChannelHandler.Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<String> {

    private final ExecutorService businessLogicExecutor = Executors.newFixedThreadPool(10);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        log.info("Received from client {}: {}", ctx.channel().remoteAddress(), msg);
        businessLogicExecutor.submit(() -> {
            try {
                String result = processMessage(msg);
                ctx.writeAndFlush(result);
            } catch (Exception e) {
                log.error("Error processing message", e);
                ctx.writeAndFlush("Error: " + e.getMessage());
            }
        });
    }

    private String processMessage(String msg) throws InterruptedException {
        // Simulate a long-running task
        Thread.sleep(5000); // 5 seconds
        return "Processed: " + msg;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error in Netty server handler", cause);
        ctx.close();
    }

    @PreDestroy
    public void stop() {
        businessLogicExecutor.shutdown();
    }
}
