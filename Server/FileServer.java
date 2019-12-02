/*
Ho va ten : Le Duc Toan
Ma sinh vien : 16021655
Mo ta : Chuong trinh file server xu ly cac lenh cua client
*/

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.String;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class FileServer {
  // Static variables
  private static int SERVER_PORT = 8080;
  // Methods
  public static void main(String[] args) throws IOException {
    // Create listen socket
    ServerSocket listen_sock = null;
    try {
      listen_sock = new ServerSocket(SERVER_PORT);
    } catch(IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("Create socket success ...");

    boolean running = true;
    while (running) {
      System.out.println(
        "\nServer is listenning on port " +
        SERVER_PORT +
        " <Ctrl-C> to close ..."
      );
      // Accept connection
      Socket connect_sock = null;
      try {
        connect_sock = listen_sock.accept();
        System.out.println("Accept connection " + connect_sock);
        AcceptThread c_thread = new AcceptThread(listen_sock, connect_sock);
        c_thread.start();
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
  }
}


// Thread
class AcceptThread extends Thread {
  // Variables
  private ServerSocket listen_sock = null;
  private Socket connect_sock = null;

  // Contructor
  AcceptThread(ServerSocket listen_sock, Socket connect_sock) {
    this.listen_sock = listen_sock;
    this.connect_sock = connect_sock;
  }

  // Methods
  public void run() {
    try {
      InputStream is = connect_sock.getInputStream();
      DataInputStream dis = new DataInputStream(is);
      OutputStream os = connect_sock.getOutputStream();
      DataOutputStream dos = new DataOutputStream(os);
      String command = "";
      while (!command.equals("@logout")) {
        try {
          command = dis.readUTF();
          System.out.println("Client : " + command);
          if (AcceptThread.handleCommand(command, dis, dos) < 0) {
            break;
          }
        } catch (Exception e) {
          e.printStackTrace();
          break;
        }
      }
      System.out.println("Close connection.");
      connect_sock.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static int handleCommand(
    String command, DataInputStream dis, DataOutputStream dos
  ) {
    if (command.equals("dir")) {
      System.out.println("Server : ");
      File folder = new File("SharedFolder");
      File[] files_in_folder = folder.listFiles();
      if (files_in_folder == null) {
        try {
          dos.writeUTF("");
        } catch (Exception e) {
          e.printStackTrace();
          return -1;
        }
      } else {
        for (int i = 0; i <  files_in_folder.length; i++) {
          if(files_in_folder[i].isFile()) {
            try {
              dos.writeUTF(files_in_folder[i].getName());
              System.out.println("\\" + files_in_folder[i].getName());
            } catch (Exception e) {
              e.printStackTrace();
              return -1;
            }
          }
        }
      }
    } else {
      String[] command_parts = command.split(" ");
      if (command_parts.length == 2 && command_parts[0].equals("get")) {
        File file = new File("SharedFolder/" + command_parts[1]);
        if (file.exists() && file.canRead()) {
          try {
            System.out.println("Server : File existed");
            dos.writeUTF("File existed");
            long filesize = file.length();
            System.out.println("Server : " + filesize + "(bytes)");
            dos.writeLong(filesize);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int read_bytes = 0;
            while (filesize > 0) {
              read_bytes = fis.read(buffer);
              if (read_bytes > 0) {
                dos.write(buffer, 0, read_bytes);
                filesize -= read_bytes;
              }
            }
            fis.close();
            System.out.println("Client : " + dis.readUTF());
          } catch (Exception e) {
            e.printStackTrace();
            return -1;
          }
        } else {
          try {
            System.out.println("Server : Read file false");
            dos.writeUTF("Read file false");
          } catch (Exception e) {
            e.printStackTrace();
            return -1;
          }
        }
      } else if (command_parts.length == 2 && command_parts[0].equals("put")) {
        Random rd = new Random();
        File file;
        do {
          file = new File(
            "SharedFolder/" +
            command_parts[1] +
            rd.nextInt()
          );
        } while (file.exists());
        try {
          long filesize = dis.readLong();
          System.out.println("Client : " + filesize + "(bytes)");
          FileOutputStream fos = new FileOutputStream(file.getName());
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
          System.out.println("Server : Uploaded file");
          dos.writeUTF("Uploaded file");
        } catch (IOException e) {
          e.printStackTrace();
          return -1;
        }
      }
    }
    return 0;
  }
}
