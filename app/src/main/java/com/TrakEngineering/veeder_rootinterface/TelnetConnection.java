package com.TrakEngineering.veeder_rootinterface;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by Sven Wijtmans on 1/10/2018.
 * This may b the connection protcol used by the Veeder-Root device.
 * It has been copied and I am working on porting it from
 * \\TRAKHQFILE\public\Shakil\Sources\polling service IP\PollingServiceIP 3.0.0.1 MR 1630.12.11.2017\PollingServiceIP_new\PollingService\TelnetConnection.cs - (18, 18) : public class TelnetConnection
 */

//Can't do this on the main, so use AsyncTask
class TelnetConnection {
    private Socket tcpSocket;

    private int TimeOutMs = 5000;

    public TelnetConnection(String Hostname, int Port) {
        try {
            tcpSocket = new Socket(InetAddress.getByName(Hostname), Port);
            tcpSocket.setSoTimeout(TimeOutMs);
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    public String Login(String Username, String Password, int LoginTimeOutMs) {
        String errorlogin = "";
        String s = Read();
        Boolean LoginAuth = false;

        try {
            int oldTimeOutMs = TimeOutMs;
            TimeOutMs = LoginTimeOutMs;

            if (LoginAuth) {
                if (!s.replaceAll("\\s+$", "").endsWith(":"))
                    throw new Exception("Failed to connect : no login prompt");
                WriteLine(Username);

                s += Read();
                if (!s.replaceAll("\\s+$", "").endsWith(":"))
                    throw new Exception("Failed to connect : no password prompt");
                WriteLine(Password);
            }

            s += Read();
            TimeOutMs = oldTimeOutMs;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }

    public String NoLogin(String Username, String Password, int LoginTimeOutMs) {
        String errorlogin = "";
        String s = Read();


        try {
            int oldTimeOutMs = TimeOutMs;
            TimeOutMs = LoginTimeOutMs;

            s += Read();
            TimeOutMs = oldTimeOutMs;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }

    public void WriteLine(String cmd) {
        Write(cmd + "\n");
    }

    private void Write(String cmd) {
        try {
            if (!tcpSocket.isConnected()) return;
            byte[] buf = cmd.replaceAll("\0xFF", "\0xFF\0xFF").getBytes();//I may need to specifyUS-ASCII);
            tcpSocket.getOutputStream().write(buf, 0, buf.length);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String Read() {
        if (!tcpSocket.isConnected()) return null;
        StringBuilder sb = new StringBuilder();
        try {
            do {
                ParseTelnet(sb);
                Thread.sleep(2000);
            } while (tcpSocket.getInputStream().available() > 0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return sb.toString();
    }

    public Boolean IsConnected() {
        return tcpSocket.isConnected();
    }

    private void ParseTelnet(StringBuilder sb) {
        try {
            while (tcpSocket.getInputStream().available() > 0) {
                int input = tcpSocket.getInputStream().read();
                switch (input) {
                    case -1:
                        break;
                    case Verbs.IAC:
                        // interpret as command
                        int inputverb = tcpSocket.getInputStream().read();
                        if (inputverb == -1) break;
                        switch (inputverb) {
                            case Verbs.IAC:
                                //literal IAC = 255 escaped, so append char 255 to String
                                sb.append(inputverb);
                                break;
                            case Verbs.DO:
                            case Verbs.DONT:
                            case Verbs.WILL:
                            case Verbs.WONT:
                                // reply to all commands with "WONT", unless it is SGA (suppres go ahead)
                                int inputoption = tcpSocket.getInputStream().read();
                                if (inputoption == -1) break;
                                tcpSocket.getOutputStream().write((byte) Verbs.IAC);
                                if (inputoption == Options.SGA)
                                    tcpSocket.getOutputStream().write(inputverb == Verbs.DO ? (byte) Verbs.WILL : (byte) Verbs.DO);
                                else
                                    tcpSocket.getOutputStream().write(inputverb == Verbs.DO ? (byte) Verbs.WONT : (byte) Verbs.DONT);
                                tcpSocket.getOutputStream().write((byte) inputoption);
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        sb.append((char) input);
                        break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //changed from enum
    static class Verbs {
        static final int WILL = 251,
                WONT = 252,
                DO = 253,
                DONT = 254,
                IAC = 255;
    }

    static class Options {
        static final int SGA = 3;
    }
}