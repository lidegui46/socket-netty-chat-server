package com.kingston.im.net;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kingston.im.ServerConfigs;
import com.kingston.im.base.ServerNode;
import com.kingston.im.base.SpringContext;
import com.kingston.im.net.codec.PacketDecoder;
import com.kingston.im.net.codec.PacketEncoder;
import com.kingston.im.net.message.PacketType;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

public class ChatServer implements ServerNode {

    private Logger logger = LoggerFactory.getLogger(ChatServer.class);

    // 避免使用默认线程数参数
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());

    private int port;

    @Override
    public void init() {
        ServerConfigs serverConfigs = SpringContext.getServerConfigs();
        this.port = serverConfigs.getSocketPort();
    }

    @Override
    public void start() throws Exception {
        logger.info("服务端已启动，正在监听用户的请求......");
        // 协议初始化
        PacketType.initPackets();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    // 阻塞IO
                    .channel(NioServerSocketChannel.class)
                    // bossGroup 队列大小
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    // workerGroup 处理器
                    .childHandler(new ChildChannelHandler());

            // 绑定服务端端口
            serverBootstrap.bind(new InetSocketAddress(port)).sync();
        } catch (Exception e) {
            logger.error("", e);
            throw e;
        }
    }

    @Override
    public void shutDown() throws Exception {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            // 优雅关闭
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        /**
         * 管道处理流程：ChannelPipeline -> ChannelHandlerContext -> ChannelHandler -> Inbound VS Outbound（入站和出站）
         *
         * <pre>
         *     入站：接收消息
         *     出站：发送消息
         * </pre>
         *
         * @param socketChannel socket通道
         * @throws Exception
         */
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            ChannelPipeline pipeline = socketChannel.pipeline();
            // 【入站】【解码】通过“自定义长度解码器“解决TCP黏包问题
            pipeline.addLast(new PacketDecoder(1024 * 4, 0, 4, 0, 4));
            // 【入站】【心跳】客户端300秒没收发包，便会触发UserEventTriggered事件到MessageTransportHandler
            pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, 300));
            // 【入站】【读取客户端内容】
            pipeline.addLast(new IoHandler());


            // 【出站】【填充消息头长度，占位】当协议中的第一个字段为长度字段时，LengthFieldPrepender编码器可以计算当前待发送消息的二进制字节长度，将该长度添加到ByteBuf的缓冲区头中
            pipeline.addLast(new LengthFieldPrepender(4));
            // 【出站】【编码】编码处理器
            pipeline.addLast(new PacketEncoder());
        }
    }

}
