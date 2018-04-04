package chatsystem;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

public class ChatServer implements Runnable{

    private Selector selector;
    private SelectionKey serverKey;
    private Vector<String> usernames;
    private static final int PORT = 9999;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ChatServer(){
        usernames = new Vector<String>();
        init();
    }

    public void init(){
        try {
            selector = Selector.open();
            //����serverSocketChannel
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            ServerSocket socket = serverChannel.socket();
            socket.bind(new InetSocketAddress(PORT));
            //���뵽selector��
            serverChannel.configureBlocking(false);
            serverKey = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            printInfo("server starting.......");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while(true){
                //��ȡ����channel
                int count = selector.select();
                if(count > 0){
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();

                        //����key��ͨ���ǵȴ������µ��׽�������
                        if(key.isAcceptable()){
                            System.out.println(key.toString() + " : ����");
                            //һ��Ҫ�����accpet״̬�ķ�����keyȥ������������
                            iterator.remove();
                            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                            //����socket
                            SocketChannel socket = serverChannel.accept();
                            socket.configureBlocking(false);
                            //��channel���뵽selector�У���һ��ʼ��ȡ����
                            socket.register(selector, SelectionKey.OP_READ);
                        }
                        //����key��ͨ���������ݿɶ�״̬
                        if(key.isValid() && key.isReadable()){
                            System.out.println(key.toString() + " : ��");
                            readMsg(key);
                        }
                        //����key��ͨ����д����״̬
                        if(key.isValid() && key.isWritable()){
                            System.out.println(key.toString() + " : д");
                            writeMsg(key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMsg(SelectionKey key) {
        SocketChannel channel = null;
        try {
            channel = (SocketChannel) key.channel();
            //����buffer������
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            //����ͻ��˹ر���ͨ���������ڶԸ�ͨ��read���ݣ��ᷢ��IOException������Exception�󣬹رյ���channel��ȡ������key
            int count = channel.read(buffer);
            StringBuffer buf = new StringBuffer();
            //�����ȡ��������
            if(count > 0){
                //��buffer��ת����buffer�е����ݶ�ȡ����
                buffer.flip();
                buf.append(new String(buffer.array(), 0, count));
            }
            String msg = buf.toString();

            //����������ǿͻ�������ʱ���͵�����
            if(msg.indexOf("open_") != -1){
                String name = msg.substring(5);//ȡ������
                printInfo(name + " --> online");
                usernames.add(name);
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while(iter.hasNext()){
                    SelectionKey skey = iter.next();
                    //�����Ƿ������׽���ͨ����key�����������õ���key��  
                    //�����´�key����Ȥ�Ķ���  
                    if(skey != serverKey){
                        skey.attach(usernames);
                        skey.interestOps(skey.interestOps() | SelectionKey.OP_WRITE);
                    }
                }
                //���������ʱ���͵�����
            }else if(msg.indexOf("exit_") != -1){
                String username = msg.substring(5);
                usernames.remove(username);
                key.attach("close");
                //Ҫ�˳��ĵ�ǰchannel����close�ı�ʾ��������ȤתΪд�����write���յ���close�����ж�channel������
                key.interestOps(SelectionKey.OP_WRITE);
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while(iter.hasNext()){
                    SelectionKey sKey = iter.next();
                    sKey.attach(usernames);
                    sKey.interestOps(sKey.interestOps() | SelectionKey.OP_WRITE);
                }
                //��������췢������
            }else{
                String uname = msg.substring(0, msg.indexOf("^"));
                msg = msg.substring(msg.indexOf("^") + 1);
                printInfo("("+uname+")˵��" + msg);
                String dateTime = sdf.format(new Date());
                String smsg = uname + " " + dateTime + "\n  " + msg + "\n";
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while(iter.hasNext()){
                    SelectionKey sKey = iter.next();
                    sKey.attach(smsg);
                    sKey.interestOps(sKey.interestOps() | SelectionKey.OP_WRITE);
                }
            }
            buffer.clear();
        } catch (IOException e) {
            //���ͻ��˹ر�channelʱ�����������ͨ����������д������ݣ����ᱨIOException����������ǣ��ڷ�������ﲶ�������쳣�����ҹرյ��������ߵ�Channelͨ��
            key.cancel();
            try {
                channel.socket().close();
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void writeMsg(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            Object attachment = key.attachment();
            //��ȡkey��ֵ֮��Ҫ��key��ֵ�ÿգ�����Ӱ����һ�ε�ʹ��
            key.attach("");
            channel.write(ByteBuffer.wrap(attachment.toString().getBytes()));
            key.interestOps(SelectionKey.OP_READ);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printInfo(String str) {
        System.out.println("[" + sdf.format(new Date()) + "] -> " + str);
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        new Thread(server).start();
    }
}