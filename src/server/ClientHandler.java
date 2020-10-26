package server;

import javax.crypto.spec.PSource;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    DataInputStream in;
    DataOutputStream out;
    Server server;
    Socket socket;

    private String nickname;
    private String login;

    public String getNickname() {
        return nickname;
    }

    public ClientHandler(Server server, Socket socket) {


        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Client connected" + socket.getRemoteSocketAddress());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/auth ")) {
                            String[] token = str.split("\\s");
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService().
                                    getNicknameByLoginAndPassword(token[1], token[2]);
                            if (newNick != null) {
                                nickname = newNick;
                                server.subscribe(this);
                                System.out.println("Прошли подключение");
                                sendMsg("/authok " + newNick);
                                break;
                            } else {
                                sendMsg("Неверная пара логин/пароль");
                            }
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            System.out.println("Выходим");
                            out.writeUTF("/end");
                            sendMsg("/end");
                            break;
                        }

                        if (str.startsWith("/w ")) {
                            System.out.println("Зашли");
                            String[] receiver = str.split(" ");
                            if (receiver.length > 2) {
                                str = "";
                                for (int i = 2; i < receiver.length; i++) {
                                    str += receiver[i];
                                    str += " ";
                                }
                                System.out.println(str);
                                System.out.println("Кому шлем: "+ receiver[1]);
                                server.sendMsgToReceiver(this, receiver[1], str.trim());
                                continue;
                            }
                        }
                        server.broadcastMsg(this, str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("Client disconnected" + socket.getRemoteSocketAddress());
                    try {
                        socket.close();
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
