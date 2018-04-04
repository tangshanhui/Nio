package chatsystem;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ChatClient {

    private static final String HOST = "127.0.0.1";
    private static int PORT = 9999;
    private static SocketChannel socket;
    private static ChatClient client;

    private static byte[] lock = new byte[1];
    //����ģʽ����
    private ChatClient() throws IOException{
        socket = SocketChannel.open();
        socket.connect(new InetSocketAddress(HOST, PORT));
        socket.configureBlocking(false);
    }

    public static ChatClient getIntance(){
        synchronized(lock){
            if(client == null){
                try {
                    client = new ChatClient();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return client;
        }
    }

    public void sendMsg(String msg){
        try {
            socket.write(ByteBuffer.wrap(msg.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receiveMsg(){
        String msg = null;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            StringBuffer buf = new StringBuffer();
            int count = 0;
            //��һ��һ�ξ��ܶ�����������
            while((count = socket.read(buffer)) > 0){
                buf.append(new String(buffer.array(), 0, count));
            }
            //������
            if(buf.length() > 0){
                msg = buf.toString();
                if(buf.toString().equals("close")){
                    //������sleep�ᵼ��ioException�ķ���,��Ϊ�������ֱ�ӹرյ�ͨ������server�
                    //��channel��read��buffer��ʱ�ᷢ����ȡ�쳣��ͨ��sleepһ��ʱ�䣬ʹ�÷�����Ǳߵ�channel�ȹرգ��ͻ���
                    //��channel��رգ��������ܷ�ֹread(buffer)��ioException
                    //��������һ�ֱ�����
                    //Thread.sleep(100);
                    //���õķ����ǣ���readBuffer�в����쳣���ֶ����йر�ͨ��
                    socket.socket().close();
                    socket.close();
                    msg = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }
}