package com.xiaoleilu.loServer;

import com.xiaoleilu.hutool.util.DateUtil;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.action.ClassUtil;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.AdminActionHandler;
import com.xiaoleilu.loServer.handler.IMActionHandler;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * LoServer starter<br>
 * 用于启动服务器的主对象<br>
 * 使用LoServer.start()启动服务器<br>
 * 服务的Action类和端口等设置在ServerSetting中设置
 *
 * @author Looly
 */
public class LoServer {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(LoServer.class);
    private int port;
    private int adminPort;
    private IMessagesStore messagesStore;
    private ISessionsStore sessionsStore;
    private Channel channel;
    private Channel adminChannel;

    public LoServer(int port, int adminPort, IMessagesStore messagesStore, ISessionsStore sessionsStore) {
        this.port = port;
        this.adminPort = adminPort;
        this.messagesStore = messagesStore;
        this.sessionsStore = sessionsStore;
    }

    /**
     * 启动服务
     */
    public void start() throws InterruptedException {
        long start = System.currentTimeMillis();

        // Configure the server.
        final EventLoopGroup bossGroup = new NioEventLoopGroup(2);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        registerAllAction();

        try {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();
            final ServerBootstrap adminServerBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240) // 服务端可连接队列大小
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 64)
                .childOption(ChannelOption.SO_RCVBUF, 1024 * 64)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new HttpRequestDecoder());
                        socketChannel.pipeline().addLast(new HttpResponseEncoder());
                        socketChannel.pipeline().addLast(new ChunkedWriteHandler());
                        socketChannel.pipeline().addLast(new HttpObjectAggregator(100 * 1024 * 1024));
                        socketChannel.pipeline().addLast(new IMActionHandler(messagesStore, sessionsStore));
                    }
                });

            channel = serverBootstrap.bind(port).sync().channel();


            adminServerBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240) // 服务端可连接队列大小
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 64)
                .childOption(ChannelOption.SO_RCVBUF, 1024 * 64)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new HttpRequestDecoder());
                        socketChannel.pipeline().addLast(new HttpResponseEncoder());
                        socketChannel.pipeline().addLast(new ChunkedWriteHandler());
                        socketChannel.pipeline().addLast(new HttpObjectAggregator(100 * 1024 * 1024));
                        socketChannel.pipeline().addLast(new AdminActionHandler(messagesStore, sessionsStore));
                    }
                });

            adminChannel = adminServerBootstrap.bind(adminPort).sync().channel();
            Logger.info("***** Welcome To LoServer on port [{},{}], startting spend {}ms *****", port, adminPort, DateUtil.spendMs(start));
        } finally {

        }
    }

    public void shutdown() {
        if (this.channel != null) {
            this.channel.close();
            try {
                this.channel.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (this.adminChannel != null) {
            this.adminChannel.close();
            try {
                this.adminChannel.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerAllAction() {
        try {
            for (Class cls : ClassUtil.getAllAssignedClass(Action.class)) {
                if (cls.getAnnotation(Route.class) != null) {
                    ServerSetting.setAction((Class<? extends Action>) cls);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
