/*
Họ và tên : Lê Đức Toàn
Mã sinh viên : 16021655
Mô tả : Chương trình file client cho phép thao tác một số lệnh với file trên server
*/

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.InterruptedException;
import java.lang.String;
import java.net.Socket;
import java.util.Scanner;

public class FileClient {
  // Static variables
  private static int SERVER_PORT = 8080;
  // Methods
  public static void main(String[] args) throws IOException {
    Scanner sc = new Scanner(System.in);
    // Get server IP
    String server_ip = "";
    System.out.print("Server IP : ");
    server_ip = sc.nextLine();
    // Connect to server
    Socket connect_sock = null;
    try {
      connect_sock = new Socket(server_ip, SERVER_PORT);
      System.out.println("Connect to server success ...");
      InputStream is = connect_sock.getInputStream();
      DataInputStream dis = new DataInputStream(is);
      OutputStream os = connect_sock.getOutputStream();
      DataOutputStream dos = new DataOutputStream(os);
      // Handle Command
      String command = "";
      while(!command.equals("@logout")) {
        System.out.print("Client : ");
        command = sc.nextLine();
        if (FileClient.handleCommand(command, dis, dos) < 0) {
          break;
        }
      }
      System.out.println("Close connection.");
      connect_sock.close();
    } catch(IOException e) {
      e.printStackTrace();
    } finally {
      if (connect_sock != null) {
        connect_sock.close();
      }
    }
  }

  private static int handleCommand(
    String command, DataInputStream dis, DataOutputStream dos
  ) {
    String[] command_parts = command.split(" ");
    if (command_parts.length == 1 && command_parts[0].equals("dir")) {
      try {
        dos.writeUTF(command);
        Thread.sleep(100);
        System.out.println("Server : ");
        String filename = "";
        while (dis.available() > 0) {
          filename = dis.readUTF();
          System.out.println("\\" + filename);
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        return -1;
      }
    } else if (command_parts.length == 2 && command_parts[0].equals("get")) {
      try {
        dos.writeUTF(command);
        System.out.print("Server : ");
        String file_status = dis.readUTF();
        System.out.println(file_status);
        if (file_status.equals("File existed")) {
          long filesize = dis.readLong();
          System.out.println("Server : " + filesize + "(bytes)");
          FileOutputStream fos = new FileOutputStream(command_parts[1]);
          byte[] buffer = new byte[1024];
          int read_bytes = 0;
          while (filesize > 0) {
            read_bytes = dis.read(buffer);
            if (read_bytes > 0) {
              fos.write(buffer, 0, read_bytes);
              filesize -= read_bytes;
            }
          }
          fos.close();
          System.out.println("Client : Downloaded file");
          dos.writeUTF("Downloaded file");
        }
      } catch (IOException e) {
        e.printStackTrace();
        return -1;
      }
    } else if (
      command_parts.length == 1 && command_parts[0].equals("@logout")
      ) {
        try {
          dos.writeUTF(command_parts[0]);
        } catch(Exception e) {
          e.printStackTrace();
          return -1;
        }
    } else {
      System.out.println("Unknown command!");
    }
    return 0;
  }
}
